package com.r3.sgx.djvm

import com.google.protobuf.ByteString
import com.r3.sgx.core.common.ChannelInitiatingHandler
import com.r3.sgx.core.common.Sender
import com.r3.sgx.core.host.EnclaveHandle
import com.r3.sgx.djvm.handlers.HostHandler
import com.r3.sgx.dynamictesting.EnclaveTestMode
import com.r3.sgx.dynamictesting.TestEnclavesBasedTest
import com.r3.sgx.test.enclave.TestEnclave
import com.r3.sgx.test.enclave.messages.MessageType
import com.r3.sgx.test.proto.ByteCodeRequest
import com.r3.sgx.test.proto.SendJar
import com.r3.sgx.testing.RootHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.function.Consumer

class ByteCodeGenerationTests : TestEnclavesBasedTest(EnclaveTestMode.Native) {

    companion object {
        private var isEnclaveInitialized = false
        private lateinit var enclaveHandle: EnclaveHandle<RootHandler.Connection>
        private val hostHandler = HostHandler()
        private lateinit var enclaveSender: Sender


        @Suppress("unused")
        @JvmStatic
        @AfterAll
        fun destroy() {
            assertThat(isEnclaveInitialized).isTrue()
            enclaveSender.send(Int.SIZE_BYTES, Consumer { buffer ->
                buffer.putInt(MessageType.CLEAR_JARS.ordinal)
            })
//            enclaveHandle.destroy()
        }
    }

    @BeforeEach
    fun setUp() {
        if (!isEnclaveInitialized) {
            enclaveHandle = createEnclaveWithHandler(RootHandler(), TestEnclave::class.java, File(EnclaveTests.enclavePath))

            val connection = enclaveHandle.connection
            val channels = connection.addDownstream(ChannelInitiatingHandler())
            val (_, sender) = channels.addDownstream(hostHandler).get()
            enclaveSender = sender

            val userJar = HostTests.mathsJarPath.toFile().readBytes()
            val sendJarBytes = SendJar.newBuilder()
                    .setData(ByteString.copyFrom(userJar))
                    .build()
                    .toByteArray()
            sender.send(Int.SIZE_BYTES + sendJarBytes.size, Consumer { buffer ->
                buffer.putInt(MessageType.JAR.ordinal)
                buffer.put(sendJarBytes)
            })

            isEnclaveInitialized = true
        }
    }

    /**
     * Auxiliary code when comparing the DJVM generated bytecode between different JVMs
     */
    @Test
    fun testByteCodeGeneration() {
        val className = "java.lang.Math"
        val byteCodeRequest = ByteCodeRequest.newBuilder().setClassName(className).build().toByteArray()
        enclaveSender.send(Int.SIZE_BYTES + byteCodeRequest.size, Consumer { buffer ->
            buffer.putInt(MessageType.BYTECODE_DUMP.ordinal)
            buffer.put(byteCodeRequest)
        })
        assertThat(hostHandler.classesByteCodeReceived).contains("sandbox/$className".replace(".","/"))
    }

}