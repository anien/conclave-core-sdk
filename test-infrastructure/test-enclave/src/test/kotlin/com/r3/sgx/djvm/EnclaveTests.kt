package com.r3.sgx.djvm

import com.google.protobuf.ByteString
import com.r3.sgx.core.common.ChannelInitiatingHandler
import com.r3.sgx.core.common.Sender
import com.r3.sgx.core.host.EnclaveHandle
import com.r3.sgx.djvm.HostTests.Companion.testCodeJarPath
import com.r3.sgx.djvm.handlers.HostHandler
import com.r3.sgx.dynamictesting.EnclaveTestMode
import com.r3.sgx.dynamictesting.TestEnclavesBasedTest
import com.r3.sgx.test.EnclaveJvmTest
import com.r3.sgx.test.enclave.TestEnclave
import com.r3.sgx.test.enclave.messages.MessageType
import com.r3.sgx.test.loadTestClasses
import com.r3.sgx.test.proto.ExecuteTest
import com.r3.sgx.test.proto.SendJar
import com.r3.sgx.testing.RootHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import java.net.URLClassLoader
import java.util.function.Consumer
import java.util.stream.Stream

/**
 * Run tests in the enclave, inside and outside of the DJVM sandbox
 */
class EnclaveTests : TestEnclavesBasedTest(EnclaveTestMode.Native) {

    companion object {
        val enclavePath = System.getProperty("enclave.path")
                ?: throw AssertionError("System property 'enclave.path' not set.")

        private val testCodeJar = HostTests.testCodeJarPath

        private var isEnclaveInitialized = false
        private val hostHandler = HostHandler()
        private lateinit var enclaveHandle: EnclaveHandle<RootHandler.Connection>
        private lateinit var enclaveSender: Sender
        private lateinit var testClasses : List<Class<in EnclaveJvmTest>>

        @Suppress("unused")
        @AfterAll
        @JvmStatic
        fun destroy() {
            assertThat(isEnclaveInitialized).isTrue()
            enclaveSender.send(Int.SIZE_BYTES, Consumer { buffer ->
                buffer.putInt(MessageType.CLEAR_JARS.ordinal)
            })
//            enclaveHandle.destroy()
            assertThat(hostHandler.assertedTests).containsAll(testClasses.flatMap { listOf(it.name) })
            assertThat(hostHandler.assertedSandboxedTests).containsAll(testClasses.flatMap { listOf(it.name) })
        }

        @JvmStatic
        fun runTest(testClass: Class<in EnclaveJvmTest>, type: MessageType, sender: Sender) {
            val test = testClass.newInstance() as EnclaveJvmTest
            val input = test.getTestInput()
            val executeTestBuilder = ExecuteTest.newBuilder()
                    .setClassName(testClass.name)
            input?.let { executeTestBuilder.setInput(ByteString.copyFrom(it)) }

            val executeTestBytes = executeTestBuilder.build().toByteArray()
            sender.send(Int.SIZE_BYTES + executeTestBytes.size, Consumer { buffer ->
                buffer.putInt(type.ordinal)
                buffer.put(executeTestBytes)
            })
        }
    }

    @BeforeEach
    fun setUp() {
        if (!isEnclaveInitialized) {
            enclaveHandle = createEnclaveWithHandler(RootHandler(), TestEnclave::class.java, File(enclavePath))
            val connection = enclaveHandle.connection
            val channels = connection.addDownstream(ChannelInitiatingHandler())
            val (_, sender) = channels.addDownstream(hostHandler).get()
            enclaveSender = sender

            val message = SendJar.newBuilder()
                    .setData(ByteString.copyFrom(testCodeJar.toFile().readBytes()))
                    .build().toByteArray()
            sender.send(Int.SIZE_BYTES + message.size, Consumer { buffer ->
                buffer.putInt(MessageType.JAR.ordinal)
                buffer.put(message)
            })

            isEnclaveInitialized = true
        }
    }

    class ClassArgumentProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            val testsJars = arrayOf(testCodeJarPath.toUri().toURL())
            testClasses = loadTestClasses(URLClassLoader(testsJars), testsJars)
            return testClasses.stream().map { Arguments.of(it) }
        }

    }

    @ArgumentsSource(ClassArgumentProvider::class)
    @ParameterizedTest(name = "{index} => {0}")
    fun runEnclaveTests(testClass: Class<in EnclaveJvmTest>) {
        runTest(testClass, MessageType.TEST, enclaveSender)
        assertThat(hostHandler.assertedTests).contains(testClass.name)
    }

    @ArgumentsSource(ClassArgumentProvider::class)
    @ParameterizedTest(name = "{index} => {0}")
    fun runSandboxedEnclaveTests(testClass: Class<in EnclaveJvmTest>) {
        runTest(testClass, MessageType.SANDBOX_TEST, enclaveSender)
        assertThat(hostHandler.assertedSandboxedTests).contains(testClass.name)
    }
}