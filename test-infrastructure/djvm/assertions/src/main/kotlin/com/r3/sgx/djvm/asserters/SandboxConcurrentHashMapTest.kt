package com.r3.sgx.djvm.asserters

import com.google.protobuf.Int32Value
import com.r3.sgx.test.assertion.TestAsserter
import org.assertj.core.api.Assertions.assertThat

class SandboxConcurrentHashMapTest {

    class TestJoiningIterableInsideSandbox : TestAsserter {
        override fun assertResult(testResult: ByteArray) {
            val result = String(testResult)
            assertThat(result).isEqualTo("[one has 3]")
        }
    }

    class TestStreamOfKeys : TestAsserter {
        override fun assertResult(testResult: ByteArray) {
            val result = Int32Value.parseFrom(testResult).value
            assertThat(result).isEqualTo(6)
        }
    }
}