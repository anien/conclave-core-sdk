package com.r3.sgx.testing

import com.r3.sgx.core.common.*
import java.nio.ByteBuffer
import java.util.function.Consumer

/**
 * A common base [Handler] that handles errors and muxes additional functionality.
 */
class RootHandler : Handler<RootHandler.Connection> {
    private val errorHandler = ThrowingErrorHandler()
    private val muxingHandler = SimpleMuxingHandler()

    override fun onReceive(connection: Connection, input: ByteBuffer) {
        errorHandler.onReceive(connection.errorConnection, input)
    }

    override fun connect(upstream: Sender): Connection {
        val errorConnected = errorHandler.connect(upstream)
        val muxConnected = errorConnected.addDownstream(muxingHandler)
        return Connection(errorConnected, muxConnected)
    }

    class Connection(
        val errorConnection: ErrorHandler.Connection,
        val muxConnection: SimpleMuxingHandler.Connection
    ) {
        fun <CONNECTION> addDownstream(downstream: Handler<CONNECTION>): CONNECTION {
            return muxConnection.addDownstream(downstream)
        }
    }
}
