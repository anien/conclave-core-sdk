package com.r3.conclave.testing

import com.r3.conclave.common.internal.getRemainingBytes
import com.r3.conclave.common.internal.handler.Handler
import com.r3.conclave.common.internal.handler.Sender
import java.nio.ByteBuffer
import java.util.function.Consumer

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
        upstream.send(bytes.size, Consumer { buffer ->
            buffer.put(bytes)
        })
    }
}