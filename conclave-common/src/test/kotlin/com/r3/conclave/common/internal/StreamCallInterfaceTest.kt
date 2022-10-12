package com.r3.conclave.common.internal

import com.r3.conclave.utilities.internal.getRemainingString
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.InputStream
import java.io.OutputStream
import java.lang.IllegalStateException
import java.net.ConnectException
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.Semaphore

/**
 * Test the stream call interface classes.
 * It should be noted that the use of [EnclaveCallType] and [HostCallType] in these tests isn't important.
 * There is no enclave or host present for any of these tests and the enums are re-used solely for convenience.
 */
class StreamCallInterfaceTest {
    companion object {
        private const val SOCKET_PORT_NUMBER = 31893
        private const val ENCLAVE_HOST_INTERFACE_THREADS = 8
    }

    private lateinit var hostEnclaveInterface: CallInterface<EnclaveCallType, HostCallType>
    private lateinit var enclaveHostInterface: CallInterface<HostCallType, EnclaveCallType>

    private lateinit var hostSocket: Socket
    private lateinit var enclaveSocket: Socket

    @BeforeEach
    fun setup() {
        setupSockets()
        setupInterfaces()
    }

    /** Create a pair of sockets that are connected together. */
    private fun setupSockets() {
        val serverSocketReady = Semaphore(0)

        val serverSocketThread = Thread {
            ServerSocket(SOCKET_PORT_NUMBER).use { serverSocket ->
                serverSocketReady.release()
                hostSocket = serverSocket.accept()
            }
        }.apply { start() }

        serverSocketReady.acquire()

        for (retries in 0..10) {
            try {
                enclaveSocket = Socket("127.0.0.1", SOCKET_PORT_NUMBER)
                break
            } catch (e: ConnectException) {
                Thread.sleep(5)
            }
        }

        serverSocketThread.join()
    }

    /** Use reflection to instantiate the call interfaces, as they are not part of the common package. */
    private fun setupInterfaces() {
        val hostEnclaveInterfaceClass = Class.forName("com.r3.conclave.host.internal.StreamHostEnclaveInterface")
        val enclaveHostInterfaceClass = Class.forName("com.r3.conclave.enclave.internal.StreamEnclaveHostInterface")

        val hostEnclaveInterfaceConstructor = hostEnclaveInterfaceClass.getDeclaredConstructor(
                OutputStream::class.java, InputStream::class.java)
        val enclaveHostInterfaceConstructor = enclaveHostInterfaceClass.getDeclaredConstructor(
                OutputStream::class.java, InputStream::class.java, Int::class.java)

        hostEnclaveInterface = hostEnclaveInterfaceConstructor.newInstance(
                hostSocket.getOutputStream(), hostSocket.getInputStream()) as CallInterface<EnclaveCallType, HostCallType>
        enclaveHostInterface = enclaveHostInterfaceConstructor.newInstance(
                enclaveSocket.getOutputStream(), enclaveSocket.getInputStream(), ENCLAVE_HOST_INTERFACE_THREADS) as CallInterface<HostCallType, EnclaveCallType>
    }

    /** Configure the enclave call interface for basic calls with an arbitrary task */
    private fun configureEnclaveHostInterfaceAction(action: (ByteBuffer) -> ByteBuffer?) {
        enclaveHostInterface.registerCallHandler(EnclaveCallType.CALL_MESSAGE_HANDLER, object : CallHandler {
            override fun handleCall(parameterBuffer: ByteBuffer): ByteBuffer? {
                return action(parameterBuffer)
            }
        })
    }

    /** Configure the host call interface for basic calls with an arbitrary task */
    private fun configureHostEnclaveInterfaceAction(action: (ByteBuffer) -> ByteBuffer?) {
        hostEnclaveInterface.registerCallHandler(HostCallType.CALL_MESSAGE_HANDLER, object : CallHandler {
            override fun handleCall(parameterBuffer: ByteBuffer): ByteBuffer? {
                return action(parameterBuffer)
            }
        })
    }

    /** This method sets up the call interfaces to perform a recursive fibonacci computation */
    private fun configureInterfacesForRecursiveFibonacci() {
        configureHostEnclaveInterfaceAction {
            when (val index = it.int) {
                0 -> 0
                1 -> 1
                else -> {
                    val a = hostEnclaveInterface.executeOutgoingCall(EnclaveCallType.CALL_MESSAGE_HANDLER, (index - 1).toByteBuffer())!!.int
                    val b = hostEnclaveInterface.executeOutgoingCall(EnclaveCallType.CALL_MESSAGE_HANDLER, (index - 2).toByteBuffer())!!.int
                    a + b
                }
            }.toByteBuffer()
        }

        configureEnclaveHostInterfaceAction {
            when (val index = it.int) {
                0 -> 0
                1 -> 1
                else -> {
                    val a = enclaveHostInterface.executeOutgoingCall(HostCallType.CALL_MESSAGE_HANDLER, (index - 1).toByteBuffer())!!.int
                    val b = enclaveHostInterface.executeOutgoingCall(HostCallType.CALL_MESSAGE_HANDLER, (index - 2).toByteBuffer())!!.int
                    a + b
                }
            }.toByteBuffer()
        }
    }

    /**
     * Configure the call interfaces for deep recursion.
     * This setup will recurse some number of times until reaching zero, then perform the provided action.
     */
    private fun configureInterfacesForDeepRecursion(terminatingAction: () -> ByteBuffer?) {
        configureHostEnclaveInterfaceAction {
            when (val recursionDepth = it.int) {
                0 -> terminatingAction()
                else -> hostEnclaveInterface.executeOutgoingCall(
                        EnclaveCallType.CALL_MESSAGE_HANDLER,
                        (recursionDepth - 1).toByteBuffer())
            }
        }

        configureEnclaveHostInterfaceAction {
            when (val recursionDepth = it.int) {
                0 -> terminatingAction()
                else -> enclaveHostInterface.executeOutgoingCall(
                        HostCallType.CALL_MESSAGE_HANDLER,
                        (recursionDepth - 1).toByteBuffer())
            }
        }
    }

    private fun referenceFibonacci(index: Int): Int {
        if (index < 2) return index
        val values = IntArray(index + 1)
        values[0] = 0
        values[1] = 1
        for (i in 2 .. index) {
            values[i] = values[i - 1] + values[i - 2]
        }
        return values[index]
    }

    private fun Int.toByteBuffer(): ByteBuffer {
        val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
        buffer.putInt(this)
        buffer.rewind()
        return buffer
    }

    @Test
    fun `host can call enclave`() {
        // Just echo the buffer back to the host side
        configureEnclaveHostInterfaceAction { it }

        val inputString = "Test string!"
        val inputBuffer = ByteBuffer.wrap(inputString.toByteArray())
        val outputString = hostEnclaveInterface.executeOutgoingCall(EnclaveCallType.CALL_MESSAGE_HANDLER, inputBuffer)?.getRemainingString()

        assertThat(outputString).isEqualTo(inputString)
    }

    /**
     * In native mode, threads literally call the enclave from the host and return out of it. The enclave does not have
     * any threads of its own.
     * Because of this, the enclave cannot call the host without the host first calling the enclave. To simplify the
     * implementation of the stream call interfaces, they have been written such that this remains the case
     * (even though it need not necessarily be so).
     * This test checks to see if this behaviour is intact. This helps to ensure that there are as few
     * differences between the stream and native interfaces as possible.
     */
    @Test
    fun `enclave may not call host outside the context of an enclave call`() {
        val exception = assertThrows<IllegalStateException> {
            enclaveHostInterface.executeOutgoingCall(HostCallType.CALL_MESSAGE_HANDLER)
        }
        assertThat(exception).hasMessage("Outgoing host calls may not occur outside the context of an enclave call.")
    }

    @Test
    fun `enclave can call host inside the context of an enclave call`() {
        // Enclave forwards call back to host
        configureEnclaveHostInterfaceAction {
            enclaveHostInterface.executeOutgoingCall(HostCallType.CALL_MESSAGE_HANDLER, it)
        }

        // Host handles call by echoing it
        configureHostEnclaveInterfaceAction { it }

        val inputString = "This is an input string!"
        val inputBuffer = ByteBuffer.wrap(inputString.toByteArray())
        val outputString = hostEnclaveInterface.executeOutgoingCall(EnclaveCallType.CALL_MESSAGE_HANDLER, inputBuffer)?.getRemainingString()

        assertThat(outputString).isEqualTo(inputString)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 4, 8, 16, 32])
    fun `enclave and host can perform deeply recursive calls`(recursionDepth: Int) {
        configureInterfacesForDeepRecursion { null }
        val returnValue = hostEnclaveInterface.executeOutgoingCall(EnclaveCallType.CALL_MESSAGE_HANDLER, recursionDepth.toByteBuffer())
        assertThat(returnValue).isNull()
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 4, 8, 16, 32])
    fun `exceptions propagate recursively out of the enclave`(recursionDepth: Int) {
        val exceptionMessage = "End of the line!"
        configureInterfacesForDeepRecursion { throw IllegalStateException(exceptionMessage) }

        val exception = assertThrows<IllegalStateException> {
            hostEnclaveInterface.executeOutgoingCall(EnclaveCallType.CALL_MESSAGE_HANDLER, recursionDepth.toByteBuffer())
        }

        assertThat(exception).hasMessage(exceptionMessage)
    }

    /**
     * This test implements a trivial recursive fibonacci sequence.
     * This tests recursion and cases where enclave/host calls contain multiple other enclave/host calls.
     */
    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7])
    fun `enclave and host can perform deeply recursive branching calls`(fibonacciIndex: Int) {
        configureInterfacesForRecursiveFibonacci()

        val fibonacciResult = hostEnclaveInterface.executeOutgoingCall(
                EnclaveCallType.CALL_MESSAGE_HANDLER, fibonacciIndex.toByteBuffer())!!.int

        assertThat(fibonacciResult).isEqualTo(referenceFibonacci(fibonacciIndex))
    }

    /**
     * This test implements a large number of concurrent calls.
     * This tests that the synchronisation mechanisms deal properly with contention on the host side and that return
     * values are sent back to the right caller when multiple callers are present.
     */
    @ParameterizedTest
    @ValueSource(ints = [2, 8, 256])
    fun `enclave can service multiple concurrent calls`(concurrency: Int) {
        // Sleep for the specified duration and send the duration back to the caller
        configureEnclaveHostInterfaceAction {
            val sleepFor = it.int
            Thread.sleep(sleepFor.toLong())
            sleepFor.toByteBuffer()
        }

        class SlowCountRunner(val input: Int) : Runnable {
            var finalCount: Int? = null
            var error: Throwable? = null

            override fun run() {
                try {
                    finalCount = checkNotNull(hostEnclaveInterface.executeOutgoingCall(EnclaveCallType.CALL_MESSAGE_HANDLER, input.toByteBuffer())) {
                        "No return value was received!"
                    }.int
                } catch (t: Throwable) {
                    error = t
                }
            }
        }

        val slowCountRunners = ArrayList<SlowCountRunner>(concurrency).apply {
            for (i in 0 until concurrency) {
                add(SlowCountRunner((32..64).random()))
            }
        }

        val threads = slowCountRunners.map { Thread(it) }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // Double check that calls were not mixed up
        slowCountRunners.forEach {
            assertThat(it.finalCount).isEqualTo(it.input)
        }
    }

    /**
     * This is a hammer test that implements a large number of concurrent recursive fibonacci calls.
     * The purpose of this test is to stress the message delivery and synchronisation mechanisms by causing large
     * numbers of messages to be delivered and received in multiple threads.
     */
    @ParameterizedTest
    @ValueSource(ints = [2, 8, 256])
    fun `enclave can service multiple concurrent recursive calls`(concurrency: Int) {
        configureInterfacesForRecursiveFibonacci()

        class RecursiveFibonacciRunner(val input: Int) : Runnable {
            var result: Int? = null
            var error: Throwable? = null

            override fun run() {
                try {
                    result = checkNotNull(hostEnclaveInterface.executeOutgoingCall(EnclaveCallType.CALL_MESSAGE_HANDLER, input.toByteBuffer())) {
                        "No return value was received!"
                    }.int
                } catch (t: Throwable) {
                    error = t
                }
            }
        }

        val fibonacciRunners = ArrayList<RecursiveFibonacciRunner>(concurrency).apply {
            for (i in 0 until concurrency) {
                add(RecursiveFibonacciRunner((8..12).random()))
            }
        }

        val threads = fibonacciRunners.map { Thread(it) }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // Check for correct values
        fibonacciRunners.forEach {
            assertThat(it.result).isEqualTo(referenceFibonacci(it.input))
        }
    }
}