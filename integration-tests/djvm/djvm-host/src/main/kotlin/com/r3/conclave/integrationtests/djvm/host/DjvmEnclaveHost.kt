package com.r3.conclave.integrationtests.djvm.host

import com.google.protobuf.ByteString
import com.r3.conclave.common.OpaqueBytes
import com.r3.conclave.host.AttestationParameters
import com.r3.conclave.host.EnclaveHost
import com.r3.conclave.integrationtests.djvm.base.enclave.proto.*
import com.r3.conclave.integrationtests.djvm.base.EnclaveJvmTest
import com.r3.conclave.integrationtests.djvm.base.TestSerializable
import com.r3.conclave.integrationtests.djvm.base.enclave.proto.*
import java.nio.file.Files
import java.nio.file.Path

class DjvmEnclaveHost : AutoCloseable {
    private val enclaveHost = EnclaveHost.load("com.r3.conclave.integrationtests.djvm.enclave.DjvmEnclave")

    fun start(spid: OpaqueBytes?, attestationKey: String?) {
        enclaveHost.start(AttestationParameters.EPID(spid!!, attestationKey!!), null)
    }

    fun loadJar(jarFile: Path) {
        callEnclave {
            sendJarBuilder.data = ByteString.copyFrom(Files.readAllBytes(jarFile))
        }
    }

    fun generateBytecode(className: String): BytecodeResult {
        val response = callEnclave {
            bytecodeRequestBuilder.className = className
        }
        return BytecodeResult.parseFrom(response)
    }

    fun executeTest(mode: ExecuteTest.Mode, test: EnclaveJvmTest): ByteArray {
        val response = callEnclave {
            executeTestBuilder.mode = mode
            executeTestBuilder.className = test.javaClass.name
            if (test is TestSerializable) {
                executeTestBuilder.input = ByteString.copyFrom(test.getTestInput())
            }
        }
        return TestResult.parseFrom(response!!).result.toByteArray()
    }

    private inline fun callEnclave(block: Request.Builder.() -> Unit): ByteArray? {
        val request = Request.newBuilder()
        block(request)
        return enclaveHost.callEnclave(request.build().toByteArray())
    }

    override fun close() {
        callEnclave { clearJars = ClearJars.getDefaultInstance() }
        enclaveHost.close()
    }
}
