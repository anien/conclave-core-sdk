package com.r3.conclave.common.internal.handler

import com.r3.conclave.utilities.internal.getRemainingBytes
import java.nio.ByteBuffer

/**
 * A [Handler]/[Sender] pair that sends/receives strings.
 */
abstract class StringHandler : Handler<StringSender> {
    abstract fun onReceive(sender: StringSender, string: String)

    final override fun onReceive(connection: StringSender, input: ByteBuffer) {
        onReceive(connection, String(input.getRemainingBytes()))
    }

    final override fun connect(upstream: Sender): StringSender {
        return StringSender(upstream)
    }
}

class StringSender(private val upstream: Sender) {
    fun send(string: String) {
        val bytes = string.toByteArray()
        upstream.send(bytes.size) { buffer ->
            buffer.put(bytes)
        }
    }
}