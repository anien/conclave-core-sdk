package com.r3.sgx.core.common

import com.google.protobuf.CodedInputStream
import java.lang.IllegalStateException
import java.nio.ByteBuffer

/**
 * A [Handler] that handle exceptions received from an [ExceptionSendingHandler].
 *
 * If an exception is received [onError] is called.
 */
abstract class ErrorHandler: Handler<ErrorHandler.Connection> {
    /** Handle an error raised and sent from the other side */
    abstract fun onError(throwable: Throwable)

    final override fun onReceive(connection: Connection, input: ByteBuffer) {
        val discriminator = input.get()
        when (discriminator) {
            SerializeException.Discriminator.ERROR.value -> {
                val throwable = parseException(input)
                onError(throwable)
            }

            SerializeException.Discriminator.NO_ERROR.value -> {
                val downstream = connection.downstream ?: throw IllegalStateException("Downstream not set")
                downstream.onReceive(input)
            }

            else -> throw java.lang.IllegalArgumentException("Unrecognized error discriminator")
        }
    }

    override fun connect(upstream: Sender): Connection {
        return Connection(upstream)
    }

    private fun parseException(input: ByteBuffer): Throwable {
        return try {
            val exception = Exception.parseFrom(CodedInputStream.newInstance(input))
            SerializeException.protobufToJava(exception)
        } catch (throwable: Throwable) {
            input.mark()
            val size = Integer.min(input.remaining(), 64)
            val bytes = ByteArray(size)
            input.get(bytes)
            input.reset()
            IllegalArgumentException("Cannot parse exception bytes starting with ${bytes.toList()}", throwable)
        }
    }

    class Connection(private val upstream: Sender) {
        private var _downstream: HandlerConnected<*>? = null
        val downstream: HandlerConnected<*>? get() = _downstream

        @Synchronized
        fun <CONNECTION> setDownstream(downstream: Handler<CONNECTION>): CONNECTION {
            if (this._downstream != null) {
                throw IllegalArgumentException("Can only have a single downstream")
            } else {
                val connection = downstream.connect(upstream)
                this._downstream = HandlerConnected(downstream, connection)
                return connection
            }
        }
    }
}
