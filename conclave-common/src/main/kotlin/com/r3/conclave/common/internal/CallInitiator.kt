package com.r3.conclave.common.internal

import java.nio.ByteBuffer

interface CallInitiator<CALL_ID_TYPE> {
    companion object {
        private val EMPTY_BYTE_BUFFER: ByteBuffer = ByteBuffer.wrap(ByteArray(0)).asReadOnlyBuffer()
    }

    fun executeCall(callType: CALL_ID_TYPE, parameterBuffer: ByteBuffer = EMPTY_BYTE_BUFFER): ByteBuffer?

    fun executeCallAndCheckReturn(callType: CALL_ID_TYPE, parameterBuffer: ByteBuffer = EMPTY_BYTE_BUFFER): ByteBuffer {
        return checkNotNull(executeCall(callType, parameterBuffer)) {
            "Missing return value from call '$callType'"
        }
    }
}
