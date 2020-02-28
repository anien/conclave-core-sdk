package com.r3.sgx.djvm.asserters

import com.r3.sgx.test.assertion.TestAsserter
import org.assertj.core.api.Assertions.assertThat

class SandboxCloneableTest {
    class TestCloningInsideSandbox : TestAsserter {
        override fun assertResult(testResult: ByteArray) {
            val result = String(testResult)
            assertThat(result).isEqualTo("Jango Fett")
        }
    }

    class TestFailedCloningInsideSandbox : TestAsserter {
        override fun assertResult(testResult: ByteArray) {
            val exceptionMessage = String(testResult)
            assertThat(exceptionMessage)
                    .contains("sandbox.com.r3.sgx.djvm.SandboxCloneableTest\$Jedi")
        }
    }
}