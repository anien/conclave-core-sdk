package com.r3.conclave.host

import com.google.common.annotations.VisibleForTesting
import com.google.common.io.BaseEncoding
import com.r3.conclave.common.enclave.EnclaveCall
import com.r3.conclave.common.internal.*
import com.r3.conclave.host.EnclaveHost.State.*
import com.r3.sgx.core.common.*
import com.r3.sgx.core.host.*
import com.r3.sgx.enclavelethost.server.AttestationService
import com.r3.sgx.enclavelethost.server.EnclaveletHostConfiguration
import com.r3.sgx.enclavelethost.server.internal.IntelAttestationService
import com.r3.sgx.enclavelethost.server.internal.MockAttestationService
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.function.Consumer
import kotlin.Exception

/**
 * Represents an enclave running on the local CPU. Instantiating this object loads and
 * initialises the enclave, making it ready to receive connections.
 *
 * You can get a [EnclaveHost] using one of the static factory methods.
 *
 * An enclave won't actually be loaded and initialised immediately until the [start] method is explicitly called.
 * This gives you time to configure the [EnclaveHost] before startup.
 *
 * Multiple enclaves can be loaded at once, however, you may not mix
 * simulation/debug/production enclaves together.
 *
 * Although the enclave must currently run against Java 8, the host can use any
 * version of Java that is supported.
 */
class EnclaveHost @PotentialPackagePrivate private constructor(
        private val handle: EnclaveHandle<ErrorHandler.Connection>,
        private val attestationService: AttestationService
) : AutoCloseable {
    companion object {
        // TODO Require the user to provide these
        private val r3EpidSpid = BaseEncoding.base16().decode(EnclaveletHostConfiguration.defaults.epidSpid)
        private val r3SubscriptionKey = EnclaveletHostConfiguration.defaults.iasSubscriptionKey

        private val attestationConfig = EpidAttestationHostConfiguration(
                // TODO Does the quote type need to be configurable?
                quoteType = SgxQuoteType.LINKABLE.value,
                spid = Cursor.wrap(SgxSpid, r3EpidSpid)
        )

        // This wouldn't be needed if the c'tor was package-private.
        internal fun create(handle: EnclaveHandle<ErrorHandler.Connection>, attestationService: AttestationService): EnclaveHost {
            return EnclaveHost(handle, attestationService)
        }

        @PotentialPackagePrivate
        @VisibleForTesting
        internal fun create(enclaveFile: Path, mode: EnclaveLoadMode): EnclaveHost {
            val handle = NativeHostApi(mode).createEnclave(ThrowingErrorHandler(), enclaveFile.toFile())
            val attestationService = when (mode) {
                EnclaveLoadMode.RELEASE -> IntelAttestationService("https://api.trustedservices.intel.com/sgx", r3SubscriptionKey)
                EnclaveLoadMode.DEBUG -> IntelAttestationService("https://api.trustedservices.intel.com/sgx/dev", r3SubscriptionKey)
                EnclaveLoadMode.SIMULATION -> MockAttestationService()
            }
            return create(handle, attestationService)
        }

        /**
         * Returns a [EnclaveHost] object initialised from the signed enclave
         * library file (ending in `.signed.so` on Linux) and in which the provided
         * local directory is used for sealed storage.
         *
         * @throws InvalidEnclaveException if something goes wrong during the load.
         */
        // TODO Remove the need for EnclaveMode
        @JvmStatic
        fun loadFromDisk(enclaveFile: Path, mode: EnclaveMode): EnclaveHost {
            val internalMode = when (mode) {
                EnclaveMode.RELEASE -> EnclaveLoadMode.RELEASE
                EnclaveMode.DEBUG -> EnclaveLoadMode.DEBUG
                EnclaveMode.SIMULATION -> EnclaveLoadMode.SIMULATION
            }
            try {
                return create(enclaveFile, internalMode)
            } catch (e: Exception) {
                throw InvalidEnclaveException("Unable to load enclave", e)
            }
        }

        // TODO load enclave file from memory
        // TODO load enclave file from resource
    }

    private val stateManager = StateManager<State>(New)
    private lateinit var sender: Sender

    /**
     * Causes the enclave to be loaded and the [Enclave] object constructed inside.
     * This method must be called before sending is possible. Remember to call
     * [close] to free the associated enclave resources when you're done with it.
     */
    @Throws(InvalidEnclaveException::class)
    // TODO MailHandler parameter
    fun start() {
        checkNotClosed()
        if (stateManager.state != New) return
        try {
            val mux = handle.connection.setDownstream(SimpleMuxingHandler())
            sender = mux.addDownstream(HostHandler(this))
            // TODO RA
            val attestation = mux.addDownstream(EpidAttestationHostHandler(attestationConfig))

            // We need to send an empty message to create the Enclave object and start it up.
            sender.send(0, Consumer { })
            stateManager.checkStateIs<Started>()
        } catch (e: Exception) {
            throw InvalidEnclaveException("Unable to start enclave", e)
        }
    }

    // TODO val info: EnclaveInstanceInfo

    /**
     * Passes the given byte array to the enclave. The format of the byte
     * arrays are up to you but will typically use some sort of serialization
     * mechanism, alternatively, [DataOutputStream] is a convenient way to lay out
     * pieces of data in a fixed order.
     *
     * For this method to work the enclave class must implement [EnclaveCall]. The return
     * value of [EnclaveCall.invoke] (which can be null) is returned here. It will not
     * be received via the provided callback.
     *
     * With the provided callback the enclave also has the option of using
     * [Enclave.callUntrustedHost] and sending/receiving byte arrays in the opposite
     * direction. By chaining callbacks together, a kind of virtual stack can be constructed
     * allowing complex back-and-forth conversations between enclave and untrusted host.
     *
     * @param bytes Bytes to send to the enclave.
     * @param callback Bytes received from the enclave via [Enclave.callUntrustedHost].
     *
     * @return The return value of the enclave's [EnclaveCall.invoke].
     *
     * @throws IllegalStateException If the [Enclave] does not implement [EnclaveCall]
     * or if the host has not been started.
     */
    fun callEnclave(bytes: ByteArray, callback: EnclaveCall): ByteArray? = callEnclaveInternal(bytes, callback)

    /**
     * Passes the given byte array to the enclave. The format of the byte
     * arrays are up to you but will typically use some sort of serialization
     * mechanism, alternatively, [DataOutputStream] is a convenient way to lay out
     * pieces of data in a fixed order.
     *
     * For this method to work the enclave class must implement [EnclaveCall]. The return
     * value of [EnclaveCall.invoke] (which can be null) is returned here.
     *
     * The enclave does not have the option of using [Enclave.callUntrustedHost] for
     * sending bytes back to the host. Use the overlaod which takes in a [EnclaveCall]
     * callback instead.
     *
     * @param bytes Bytes to send to the enclave.
     *
     * @return The return value of the enclave's [EnclaveCall.invoke].
     *
     * @throws IllegalStateException If the [Enclave] does not implement [EnclaveCall]
     * or if the host has not been started.
     */
    fun callEnclave(bytes: ByteArray): ByteArray? = callEnclaveInternal(bytes, null)

    private fun callEnclaveInternal(bytes: ByteArray, callback: EnclaveCall?) : ByteArray? {
        val state = stateManager.state
        if (state is Started) {
            require(state.enclaveIsEnclaveCall) { "Enclave does not implement EnclaveCall to receive messages from the host." }
        } else {
            stateManager.checkStateIsNot<New> { "The host has not been started." }
            checkNotClosed()
        }
        // It's allowed for the host to recursively call back into the enclave with callEnclave via the callback. In this
        // scenario the "state" local variable would represent the previous call into the enclave. Once this recusive step
        // is complete we restore "state" to be the current state again so that the recursion can unwind.
        val intoEnclave = CallIntoEnclave(callback)
        stateManager.state = intoEnclave
        sendToEnclave(bytes, isEnclaveCallReturn = false)
        return if (stateManager.state == intoEnclave) {
            stateManager.state = state
            null
        } else {
            val response = stateManager.transitionStateFrom<EnclaveResponse>(to = state)
            response.bytes
        }
    }

    private fun onReceive(input: ByteBuffer) {
        when (val state = stateManager.state) {
            New -> {
                // On start the host sends a (blank) message to the enclave to start it up. It responds by sending back
                // a boolean for whether it implements EnclaveCall or not. Upon receipt the host can flag itself as
                // fully started.
                val isEnclaveCall = input.getBoolean()
                input.checkNoRemaining()
                stateManager.state = Started(isEnclaveCall)
            }
            is CallIntoEnclave -> {
                // This is unpacking Enclave.sendToHost. We only expect the enclave to respond back to us with this
                // after we've first called into it using callEnclave.
                //
                // isEnclaveCallReturn tells us whether the enclave is sending back the result of its EnclaveCall.invoke
                // or if it's a call back to the host from within EnclaveCall.invoke. In the former case this is returned
                // from callEnclave, in the later case it's instead sent to the calllback provided to callEnclave.
                val isEnclaveCallReturn = input.getBoolean()
                val bytes = input.getRemainingBytes()
                if (isEnclaveCallReturn) {
                    stateManager.state = EnclaveResponse(bytes)
                } else {
                    requireNotNull(state.callback) {
                        "Enclave responded via callUntrustedHost but a callback was not provided to callEnclave."
                    }
                    val response = state.callback.invoke(bytes)
                    if (response != null) {
                        sendToEnclave(response, isEnclaveCallReturn = true)
                    }
                }
            }
            else -> throw IllegalStateException(state.toString())
        }
    }

    private fun checkNotClosed() {
        stateManager.checkStateIsNot<Closed> { "The host has been closed." }
    }

    private fun sendToEnclave(bytes: ByteArray, isEnclaveCallReturn: Boolean) {
        sender.send(1 + bytes.size, Consumer { buffer ->
            buffer.putBoolean(isEnclaveCallReturn)
            buffer.put(bytes)
        })
    }

    // TODO deliverMail

    override fun close() {
        stateManager.state = Closed
    }

    private class HostHandler(private val host: EnclaveHost) : Handler<Sender> {
        override fun connect(upstream: Sender): Sender = upstream
        override fun onReceive(connection: Sender, input: ByteBuffer) = host.onReceive(input)
    }

    private sealed class State {
        object New : State()
        class Started(val enclaveIsEnclaveCall: Boolean) : State()
        class CallIntoEnclave(val callback: EnclaveCall?) : State()
        class EnclaveResponse(val bytes: ByteArray) : State()
        object Closed : State()
    }
}

/**
 * Passes the given byte array to the enclave. The format of the byte
 * arrays are up to you but will typically use some sort of serialization
 * mechanism, alternatively, [DataOutputStream] is a convenient way to lay out
 * pieces of data in a fixed order.
 *
 * For this method to work the enclave class must implement [EnclaveCall]. The return
 * value of [EnclaveCall.invoke] (which can be null) is returned here.
 *
 * The enclave does not have the option of using [Enclave.callUntrustedHost] for
 * sending bytes back to the host. Use the overlaod which takes in a [EnclaveCall]
 * callback instead.
 *
 * @param bytes Bytes to send to the enclave.
 *
 * @return The return value of the enclave's [EnclaveCall.invoke].
 *
 * @throws IllegalStateException If the [Enclave] does not implement [EnclaveCall]
 * or if the host has not been started.
 */
fun EnclaveHost.callEnclave(bytes: ByteArray, callback: (ByteArray) -> ByteArray?): ByteArray? {
    return callEnclave(bytes, EnclaveCall { callback(it) })
}

// TODO The enclave load mode enum should not be needed. It should be possible to query the enclave mode from the .so file
//      itself, as that's what defines the mode. https://r3-cev.atlassian.net/browse/CON-13.
//      I've introduced this as a duplicate of EnclaveLoadMode to avoid exposing it in the Conclave.
enum class EnclaveMode {
    RELEASE,
    DEBUG,
    SIMULATION
}
