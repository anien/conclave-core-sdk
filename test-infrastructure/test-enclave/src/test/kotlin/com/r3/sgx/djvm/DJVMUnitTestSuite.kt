package com.r3.sgx.djvm

import com.google.protobuf.ByteString
import com.r3.sgx.core.common.ChannelInitiatingHandler
import com.r3.sgx.core.common.Sender
import com.r3.sgx.core.host.EnclaveHandle
import com.r3.sgx.djvm.handlers.HostHandler
import com.r3.sgx.dynamictesting.EnclaveTestMode
import com.r3.sgx.dynamictesting.TestEnclavesBasedTest
import com.r3.sgx.test.EnclaveJvmTest
import com.r3.sgx.test.enclave.TestEnclave
import com.r3.sgx.test.enclave.messages.MessageType
import com.r3.sgx.test.loadTestClasses
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
import java.nio.file.Paths
import java.util.function.Consumer
import java.util.stream.Stream

class DJVMUnitTestSuite : TestEnclavesBasedTest(EnclaveTestMode.Native) {

    companion object {
        val enclavePath = System.getProperty("enclave.path")
                ?: throw AssertionError("System property 'enclave_path' not set.")

        /**
         * This jar represents the custom logic loaded from a user provider jar to be run in the DJVM sandbox.
         * It is the fat jar from :djvm:internal-tests:shadowJar.
         */
        private val userJarPath = Paths.get(System.getProperty("djvm-unit-tests-jar.path")
                ?: throw AssertionError("System property 'djvm-unit-tests-jar.path' not set."))


        private var isEnclaveInitialized = false
        private val hostHandler = HostHandler()
        private lateinit var enclaveHandle: EnclaveHandle<RootHandler.Connection>
        private lateinit var enclaveSender: Sender
        private lateinit var testClasses : List<Class<in EnclaveJvmTest>>

        @JvmStatic
        @AfterAll
        fun destroy() {
            assertThat(isEnclaveInitialized).isTrue()
            enclaveSender.send(Int.SIZE_BYTES, Consumer { buffer ->
                buffer.putInt(MessageType.CLEAR_JARS.ordinal)
            })
//            enclaveHandle.destroy()
            assertThat(hostHandler.assertedDJVMTests).containsAll(testClasses.flatMap { listOf(it.name) })
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
                    .setData(ByteString.copyFrom(userJarPath.toFile().readBytes()))
                    .build().toByteArray()
            sender.send(Int.SIZE_BYTES + message.size, Consumer { buffer ->
                buffer.putInt(MessageType.JAR.ordinal)
                buffer.put(message)
            })

            isEnclaveInitialized = true
        }
    }

    class DJVMTestArgumentProvider : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            val testsJars = arrayOf(userJarPath.toUri().toURL())
            testClasses = loadTestClasses(URLClassLoader(testsJars), testsJars)
                    .filter {
                        !it.name.contains("SandboxObjectHashCodeJavaTest\$TestHashForNullObjectEnclaveTest") // ENT-4705 SIGSEGV
                    }
            return testClasses.stream().map { Arguments.of(it) }
        }
    }

    @ArgumentsSource(DJVMTestArgumentProvider::class)
    @ParameterizedTest(name = "{index} => {0}")
    fun runDJVMEnclaveTests(testClass: Class<in EnclaveJvmTest>) {
        EnclaveTests.runTest(testClass, MessageType.DJVM_TEST, enclaveSender)
        assertThat(hostHandler.assertedDJVMTests).contains(testClass.name)
    }
}