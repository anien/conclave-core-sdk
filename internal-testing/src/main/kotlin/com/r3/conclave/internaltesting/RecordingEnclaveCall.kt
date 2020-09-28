package com.r3.conclave.internaltesting

import com.r3.conclave.common.EnclaveCall

class RecordingEnclaveCall : EnclaveCall {
    val calls = ArrayList<ByteArray>()

    override fun invoke(bytes: ByteArray): ByteArray? {
        calls += bytes
        return null
    }
}