package com.r3.conclave.integrationtests.filesystem.host

import com.r3.conclave.integrationtests.filesystem.host.FilesTest.Companion.filesDelete
import com.r3.conclave.integrationtests.filesystem.host.FilesTest.Companion.filesWrite
import com.r3.conclave.integrationtests.filesystem.common.proto.Request
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.io.IOException

class JimfsInputStreamTest : FileSystemEnclaveTest() {

    private class Handler(private val uid: Int, path: String) : AutoCloseable {
        init {
            val reply = request(Request.Type.JIMFS_INPUT_STREAM_OPEN, uid = uid, path = path)
            assertThat(String(reply!!)).startsWith("com.r3.conclave.filesystem.jimfs.JimfsInputStream@")
        }

        fun readByteByByte(expectedData: ByteArray) {
            for (element in expectedData) {
                val reply = request(type = Request.Type.INPUT_STREAM_READ, uid = uid)
                assertThat(reply!!.size).isEqualTo(1)
                assertThat(reply[0]).isEqualTo(element)
            }
            val reply = request(type = Request.Type.INPUT_STREAM_READ, uid = uid)
            assertThat(reply!!.size).isEqualTo(1)
            assertThat(reply[0]).isEqualTo(-1)
        }

        fun readBytes(expectedData: ByteArray) {
            val reply = request(type = Request.Type.INPUT_STREAM_READ_BYTES, uid = uid)
            assertThat(reply).isEqualTo(expectedData)
        }

        override fun close() {
            val reply = request(type = Request.Type.INPUT_STREAM_CLOSE, uid = uid)
            assertThat(reply).isEmpty()
        }

        fun reset() {
            assertThatThrownBy { request(type = Request.Type.INPUT_STREAM_RESET, uid = uid) }
                    .isInstanceOf(java.lang.RuntimeException::class.java)
                    .hasCauseExactlyInstanceOf(IOException::class.java)
                    .hasMessageContaining("mark/reset not supported")
        }
    }

    @Test
    fun jimfsInputStreamReadResetReadBytes() {
        val path = "/jimfs.data"
        val smallFileData = byteArrayOf(1, 2, 3)
        filesWrite(path, smallFileData)
        // Create a JimfsInputStream
        Handler(uid.getAndIncrement(), path).use { inputStream ->
            // Read byte by byte
            inputStream.readByteByByte(smallFileData)
        }
        Handler(uid.getAndIncrement(), path).use { inputStream ->
            // Read all bytes at once
            inputStream.readBytes(smallFileData)
        }
        filesDelete(path)
    }

    @Test
    fun jimfsResetThrowsException() {
        val path = "/jimfs-reset.data"
        val smallFileData = byteArrayOf(1, 2, 3)
        filesWrite(path, smallFileData)
        // Create a JimfsInputStream
        Handler(uid.getAndIncrement(), path).use { inputStream ->
            inputStream.reset()
        }
        filesDelete(path)
    }
}