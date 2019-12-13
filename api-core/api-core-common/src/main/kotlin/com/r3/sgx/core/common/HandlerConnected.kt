package com.r3.sgx.core.common

import java.nio.ByteBuffer

class HandlerConnected<CONNECTION>(
        val handler: Handler<CONNECTION>,
        val connection: CONNECTION
) {
    fun onReceive(input: ByteBuffer) {
        handler.onReceive(connection, input)
    }
}
