package com.r3.conclave.host

import com.r3.conclave.common.EnclaveCall
import com.r3.conclave.enclave.Enclave
import com.r3.conclave.internaltesting.throwableWithMailCorruptionErrorMessage
import com.r3.conclave.mail.*
import com.r3.conclave.testing.MockHost
import com.r3.conclave.testing.internal.MockEnclaveEnvironment
import com.r3.conclave.utilities.internal.deserialise
import com.r3.conclave.utilities.internal.readIntLengthPrefixBytes
import com.r3.conclave.utilities.internal.writeData
import com.r3.conclave.utilities.internal.writeIntLengthPrefixBytes
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


class MailHostTest {
    companion object {
        private val messageBytes = "message".toByteArray()
    }

    private val keyPair = Curve25519KeyPairGenerator().generateKeyPair()
    private val echo by lazy { MockHost.loadMock<MailEchoEnclave>() }
    private val noop by lazy { MockHost.loadMock<NoopEnclave>() }

    @AfterEach
    fun reset() {
        MockEnclaveEnvironment.platformReset()
    }

    @Test
    fun `encrypt and deliver mail`() {
        echo.start(null, null, null)
        val mail: MutableMail = buildMail(echo)
        var response: ByteArray? = null
        echo.deliverMail(1, mail.encrypt()) { bytes ->
            response = bytes
            null  // No response back to enclave.
        }
        response!!.deserialise {
            assertArrayEquals(messageBytes, readIntLengthPrefixBytes())
            assertEquals(1, readInt())
        }
    }

    @Test
    fun `deliver mail and answer enclave`() {
        echo.start(null, null, null)
        val mail: MutableMail = buildMail(echo)
        // In response to the delivered mail, the enclave sends us a local message, and we send a local message back.
        // It asserts the answer we give is as expected.
        echo.deliverMail(1, mail.encrypt()) { "an answer".toByteArray() }
    }

    @Test
    fun `mail acknowledgement`() {
        var acknowledgementID: EnclaveMailId? = null
        echo.start(null, null, object : EnclaveHost.MailCallbacks {
            override fun acknowledgeMail(mailID: EnclaveMailId) {
                acknowledgementID = mailID
            }
        })
        val mail: MutableMail = buildMail(echo)
        // First delivery doesn't acknowledge because we don't tell it to.
        echo.deliverMail(1, mail.encrypt()) { null }
        assertNull(acknowledgementID)
        // Try again and this time we'll get an ack.
        mail.incrementSequenceNumber()
        echo.deliverMail(2, mail.encrypt()) { "acknowledge".toByteArray() }
        assertEquals(2, acknowledgementID!!)
    }

    @Test
    fun `sequence numbers`() {
        // Verify that the enclave rejects a replay of the same message, or out of order delivery.
        noop.start(null, null, null)
        val encrypted0 = buildMail(noop, "message 0".toByteArray()).encrypt()
        val encrypted1 = buildMail(noop, "message 1".toByteArray()).also { it.sequenceNumber = 1 }.encrypt()
        val encrypted2 = buildMail(noop, "message 2".toByteArray()).also { it.sequenceNumber = 2 }.encrypt()
        val encrypted50 = buildMail(noop, "message 50".toByteArray()).also { it.sequenceNumber = 50 }.encrypt()
        // Deliver message 1.
        noop.deliverMail(100, encrypted0)
        // Cannot deliver message 2 twice even with different IDs.
        noop.deliverMail(100, encrypted1)
        var msg = assertThrows<RuntimeException> { noop.deliverMail(100, encrypted1) }.message!!
        assertTrue("Highest sequence number seen is 1, attempted delivery of 1" in msg) { msg }
        // Cannot now re-deliver message 1 because the sequence number would be going backwards.
        msg = assertThrows<RuntimeException> { noop.deliverMail(100, encrypted0) }.message!!
        assertTrue("Highest sequence number seen is 1, attempted delivery of 0" in msg) { msg }
        // Can deliver message 3
        noop.deliverMail(101, encrypted2)
        // Seq nums may not have gaps.
        msg = assertThrows<RuntimeException> { noop.deliverMail(102, encrypted50) }.message!!
        assertTrue("Highest sequence number seen is 2, attempted delivery of 50" in msg) { msg }

        // Seq nums of different topics are independent
        val secondTopic = buildMail(noop).also { it.topic = "another-topic" }.encrypt()
        noop.deliverMail(100, secondTopic)
    }

    @Test
    fun corruption() {
        // Check the enclave correctly rejects messages with corrupted headers or bodies.
        noop.start(null, null, null)
        val mail = buildMail(noop)
        val encrypted = mail.encrypt()
        for (i in encrypted.indices) {
            encrypted[i]++
            assertThatThrownBy {
                noop.deliverMail(i.toLong(), encrypted)
            }.`is`(throwableWithMailCorruptionErrorMessage)
            encrypted[i]--
        }
    }

    @Test
    fun routingHint() {
        // Make a call into enclave1, which then requests sending a mail to a client with its routing hint set. Tests
        // posting mail from inside a local call using an EnclaveInstanceInfo.
        class Enclave1 : Enclave() {
            override fun receiveMail(id: EnclaveMailId, mail: EnclaveMail) {
                val outbound = createMail(mail.authenticatedSender!!, "hello".toByteArray())
                postMail(outbound, mail.from!!)
                acknowledgeMail(id)
            }
        }
        val host = MockHost.loadMock<Enclave1>()
        host.start(null, null, object : EnclaveHost.MailCallbacks {
            override fun postMail(encryptedBytes: ByteArray, routingHint: String?) {
                assertEquals("bob", routingHint!!)
                val message: EnclaveMail = Mail.decrypt(encryptedBytes, keyPair.private)
                assertEquals("hello", String(message.bodyAsBytes))
            }
        })
        val messageFromBob = buildMail(host)
        messageFromBob.from = "bob"
        host.deliverMail(1, messageFromBob.encrypt())
    }

    @Test
    fun `enclave receiving client mail for old platform version`() {
        echo.start(null, null, null)
        val encryptedMailFromClient = buildMail(echo).encrypt()
        echo.close()

        // Shutdown the enclave and "update" the platform so that we have a new CPUSVN. The new enclave's (default)
        // encryption key will be different from its old one, but we still expect the enclave to be able to decrypt it.
        MockEnclaveEnvironment.platformUpdate()

        val echo2 = MockHost.loadMock<MailEchoEnclave>()
        echo2.start(null, null, null)
        var decryptedByEnclave: ByteArray? = null
        echo2.deliverMail(1, encryptedMailFromClient) { bytes ->
            decryptedByEnclave = bytes
            null
        }

        decryptedByEnclave!!.deserialise {
            assertArrayEquals(messageBytes, readIntLengthPrefixBytes())
            assertEquals(1, readInt())
        }
    }

    @Test
    fun `enclave receiving it's own mail across platform update`() {
        class MailToSelfEnclave : Enclave(), EnclaveCall {
            override fun invoke(bytes: ByteArray): ByteArray? {
                val mail = enclaveInstanceInfo.createMail(bytes.reversedArray())
                postMail(mail, null)
                return null
            }
            override fun receiveMail(id: EnclaveMailId, mail: EnclaveMail) {
                callUntrustedHost(mail.bodyAsBytes)
            }
        }

        var mailToSelf: ByteArray? = null

        val enclave1 = MockHost.loadMock<MailToSelfEnclave>()
        enclave1.start(null, null, object : EnclaveHost.MailCallbacks {
            override fun postMail(encryptedBytes: ByteArray, routingHint: String?) {
                mailToSelf = encryptedBytes
            }
        })
        enclave1.callEnclave("secret".toByteArray())
        enclave1.close()

        MockEnclaveEnvironment.platformUpdate()

        val enclave2 = MockHost.loadMock<MailToSelfEnclave>()
        enclave2.start(null, null, null)
        var decrypted: ByteArray? = null
        enclave2.deliverMail(1, mailToSelf!!) { bytes ->
            decrypted = bytes
            null
        }

        assertThat(decrypted).isEqualTo("terces".toByteArray())
    }

    @Test
    fun `platform downgrade attack not possible`() {
        // Imagine the current platform version has a bug in it and so we update and the client creates mail from that.
        MockEnclaveEnvironment.platformUpdate()
        echo.start(null, null, null)
        val encryptedMailFromClient = buildMail(echo).encrypt()
        echo.close()

        // Let's revert the update and return the platform to it's insecure version.
        MockEnclaveEnvironment.platformDowngrade()

        val echo2 = MockHost.loadMock<MailEchoEnclave>()
        echo2.start(null, null, null)
        assertThatThrownBy {
            echo2.deliverMail(1, encryptedMailFromClient)
        }.hasMessageContaining("SGX_ERROR_INVALID_CPUSVN")
    }

    private fun buildMail(host: MockHost<*>, body: ByteArray = messageBytes): MutableMail {
        val mail = host.enclaveInstanceInfo.createMail(body)
        mail.topic = "topic-123"
        mail.sequenceNumber = 0
        mail.privateKey = keyPair.private
        return mail
    }

    class NoopEnclave : Enclave() {
        override fun receiveMail(id: EnclaveMailId, mail: EnclaveMail) {
        }
    }

    // Receives mail, decrypts it and gives the body back to the host.
    class MailEchoEnclave : Enclave() {
        override fun receiveMail(id: EnclaveMailId, mail: EnclaveMail) {
            val answer: ByteArray? = callUntrustedHost(writeData {
                writeIntLengthPrefixBytes(mail.bodyAsBytes)
                writeInt(id.toInt())
            })
            when (val str = answer?.let { String(it) }) {
                "acknowledge" -> acknowledgeMail(id)
                "an answer" -> return
                "post" -> postMail(createMail(Curve25519PublicKey(mail.bodyAsBytes), "sent to second enclave".toByteArray()), "routing hint")
                null -> return
                else -> throw IllegalStateException(str)
            }
        }
    }
}