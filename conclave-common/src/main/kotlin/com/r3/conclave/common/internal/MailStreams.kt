package com.r3.conclave.common.internal

import com.r3.conclave.common.internal.noise.protocol.*
import java.io.*
import java.nio.charset.StandardCharsets
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.BadPaddingException
import javax.crypto.ShortBufferException
import kotlin.experimental.and

/**
 * A stream filter that encrypts the input data. Closing this stream writes a termination footer which protects the
 * result against truncation attacks, so you must remember to do so.
 *
 * Every write is encrypted as a separate length-prefixed block. This stream does not perform any rechunking. It will
 * refuse to accept single-byte writes, as that would create tons of 1-byte blocks that would be very inefficient.
 * Thus this stream should always be put behind a [java.io.BufferedOutputStream] configured with a bit less
 * than the [Noise.MAX_PACKET_LEN] as the buffer size.
 *
 * The associated data must fit in the Noise handshake packet after the keys are taken into account. With Curve25519
 * this means it must be less than ([Noise.MAX_PACKET_LEN] - (32 * 8) - (16 * 8) - 1) bytes, or 65151 bytes.
 *
 * You can provide your own private key as well as the recipient's public key. The recipient will receive your public
 * key and a proof that you encrypted the message. This isn't a typical digital signature but rather is based on
 * the properties of the Diffie-Hellman algorithm - see section 7.4 of
 * [the Noise specification](http://noiseprotocol.org/noise.html#handshake-pattern-basics) for information.
 *
 * The message is encrypted with a random key each time even though both destination public key and sender private
 * keys are (expected to be) static. In other words encrypting the same message twice will yield different outputs
 * each time and when writing tests you should treat the output as if it were a stream of random numbers i.e. don't
 * compare the output against a recorded output.
 *
 * This class is not thread safe and requires external synchronization.
 *
 * @param out                  The [OutputStream] to use.
 * @param cipherName           The Noise cipher name to use, "AESGCM" is a good choice.
 * @param dhName               The Noise Diffie-Hellman algorithm name, "25519" is a good choice.
 * @param hashName             The Noise hash algorithm name, "SHA256" is a good choice.
 * @param destinationPublicKey The public key to encrypt the stream to.
 * @param associatedData       If not null, unencrypted data that will be included in the header and authenticated.
 * @param senderPrivateKey     If not null, your private key. The recipient will receive your public key and be sure you encrypted the message.
 */
internal class MailEncryptingStream(
        out: OutputStream,
        private val cipherName: String,
        private val dhName: String,
        private val hashName: String,
        // TODO: Make type safe
        destinationPublicKey: ByteArray,
        associatedData: ByteArray?,
        senderPrivateKey: ByteArray?
) : FilterOutputStream(out) {
    private var cipherState: CipherState? = null
    private val destinationPublicKey: ByteArray = destinationPublicKey.clone()
    private val associatedData: ByteArray? = associatedData?.clone()
    private val senderPrivateKey: ByteArray? = senderPrivateKey?.clone()

    /** The standard Noise protocol name, as used in the specs. */
    val protocolName = "Noise_X_${dhName}_${cipherName}_$hashName"

    // If this hasn't been written to before, emit the necessary headers to set up the Diffie-Hellman "handshake".
    // The other party isn't here to handshake with us but that's OK: they will complete it when reading the stream.
    private fun maybeHandshake() {
        if (cipherState != null) return  // Already set up the stream.

        // Noise can be used in various ways, identified by a string like this one. We always use the "X" handshake,
        // which means it's one way communication (receiver is entirely silent i.e. good for files).
        val components = protocolName.split("_").toTypedArray()
        val protocolNameBytes = protocolName.toByteArray(StandardCharsets.US_ASCII)
        val handshake = HandshakeState(protocolName, HandshakeState.INITIATOR)
        try {
            handshake.remotePublicKey.setPublicKey(destinationPublicKey, 0)

            // If one was provided, the recipient will get our public key in an authenticated manner i.e. we cannot
            // fake it because we need to use the corresponding private key when sending the message.
            val localKeyPair: DHState = handshake.localKeyPair
            if (senderPrivateKey != null) {
                localKeyPair.setPrivateKey(senderPrivateKey, 0)
            } else {
                // Otherwise we use a private key of zero. The other side will recognise the corresponding public key
                // as meaning "I am anonymous".
                localKeyPair.setPrivateKey(ByteArray(localKeyPair.privateKeyLength), 0)
            }

            // The prologue stops the Noise protocol name being tampered with to mismatch what we think we're using
            // with what the receiver thinks we're using.
            val prologue = computePrologue(protocolNameBytes)
            handshake.setPrologue(prologue, 0, prologue.size)
            handshake.start()
            assert(handshake.action == HandshakeState.WRITE_MESSAGE)

            // Check size of the associated data (i.e. unencrypted but authenticated mail headers).
            val associatedDataLen = associatedData?.size ?: 0
            val maxADLen = Noise.MAX_PACKET_LEN - localKeyPair.publicKeyLength - Noise.createCipher(components[3]).macLength - 1
            if (associatedDataLen > maxADLen)
                throw IOException("The associated data is too large: $associatedDataLen but must be less than $maxADLen")

            // The initial bytes consist of a 8 bit name length prefix, the protocol name, then a 16 bit handshake length,
            // then the Noise handshake with the authenticated payload. headerBytes is sized 8kb larger than needed to
            // provide room for keys and hashes - this is more than necessary but it doesn't matter. We could precisely
            // pre-calculate how much space is required but it's not worth it: no elliptic curve algorithm will need more
            // than this and this way we avoid accidentally running out of space/off by one errors.
            val headerBytes = ByteArray(1 + protocolName.length + 2 + 8192 + associatedDataLen)

            // Protocol name length byte.
            check(protocolName.length < 256) { "${protocolName.length} < 256" }
            headerBytes[0] = protocolName.length.toByte()

            // Copy the name.
            System.arraycopy(protocolNameBytes, 0, headerBytes, 1, protocolNameBytes.size)

            // And now pass control to Noise to write out the Diffie-Hellman handshake that sets up the key to encrypt
            // with, passing the associated data and its length. It'll be written past the two bytes we reserved to
            // record the size.
            val handshakeLength = handshake.writeMessage(headerBytes, protocolNameBytes.size + 3,
                    associatedData, 0, associatedDataLen)

            // Write two bytes of length for the handshake, now we know how big it is.
            headerBytes[1 + protocolNameBytes.size] = (handshakeLength shr 8).toByte()
            headerBytes[1 + protocolNameBytes.size + 1] = handshakeLength.toByte()

            // Write the whole header to the output stream.
            val fullHeaderLength = 1 + protocolNameBytes.size + 2 + handshakeLength
            out.write(headerBytes, 0, fullHeaderLength)

            // Now we can request the ciphering object from Noise.
            assert(handshake.action == HandshakeState.SPLIT)
            val split = handshake.split()
            split.senderOnly()   // One way not two way communication.
            cipherState = split.sender
            assert(handshake.action == HandshakeState.COMPLETE)
        } finally {
            handshake.destroy()
        }
    }

    @Throws(IOException::class)
    override fun write(b: Int) {
        throw UnsupportedOperationException("Writing individual bytes at a time is extremely inefficient. Use write(byte[]) instead.")
    }

    private val buffer = ByteArray(Noise.MAX_PACKET_LEN)

    /**
     * Writes [len] bytes from the specified [b] array starting at [off] to this output stream.
     * **The length may not be larger than [Noise.MAX_PACKET_LEN]**. Attempts to
     * write larger arrays will throw a [ShortBufferException] wrapped in an [IOException].
     *
     * @param b   the data.
     * @param off the start offset in the data.
     * @param len the number of bytes to write.
     * @throws IOException if an I/O error occurs.
     */
    @Throws(IOException::class)
    override fun write(b: ByteArray, off: Int, len: Int) {
        maybeHandshake()
        if (len == 0) return
        val cipherState = cipherState!!
        // This method should really be able to process any arbitrary length, but when wrapped in a
        // BufferedOutputStream with an appropriately sized buffer it's not really necessary.
        if (len > Noise.MAX_PACKET_LEN - cipherState.macLength - 2)
            throw IOException(ShortBufferException())
        val encryptedLength = cipherState.encryptWithAd(null, b, off, buffer, 2, len)
        buffer[0] = (encryptedLength shr 8).toByte()
        buffer[1] = encryptedLength.toByte()
        out.write(buffer, 0, encryptedLength + 2)
    }

    @Throws(IOException::class)
    override fun close() {
        // Write the terminator packet: an encryption of the empty byte array. This lets the other side know we
        // intended to end the stream and there's no MITM maliciously truncating our packets.
        maybeHandshake()
        try {
            val cipherState = cipherState!!
            val macLength = cipherState.encryptWithAd(null, ByteArray(0), 0, buffer, 2, 0)
            assert(macLength == cipherState.macLength)
            buffer[0] = (macLength shr 8).toByte()
            buffer[1] = macLength.toByte()
            out.write(buffer, 0, macLength + 2)
            out.flush()
        } catch (e: ShortBufferException) {
            throw IOException(e)
        }
        // And propagate the close.
        super.close()
    }
}

/**
 * A stream filter that decrypts a stream produced by [MailEncryptingStream].
 *
 * This stream filter will verify that the underlying stream doesn't prematurely terminate. Attempting to read from
 * it when the underlying stream has reached end-of-stream before the sender had finished writing will cause an
 * exception to be thrown, as otherwise a man in the middle could maliciously truncate the stream, possibly changing
 * its meaning.
 *
 * Mail streams protect you from a classic error in cryptography: checking the validity of a message only once it's
 * been entirely read but acting on it earlier. Mail streams won't yield any data until a full packet and its
 * authentication tag have been read and verified, so the stream cannot yield data produced by an adversary.
 * However when later bytes may change the meaning of earlier bytes you should fully consume the stream (until
 * [read] returns -1) before acting on it.
 *
 * You can access the associated (unencrypted but authenticated) data provided by the user by calling
 * [associatedData], which will read enough of the stream to provide the answer (blocking if necessary).
 *
 * Marks are not supported by this stream.
 */
internal class MailDecryptingStream(
        input: InputStream,
        privateKey: ByteArray
) : FilterInputStream(BufferedInputStream(input)) {
    private val privateKey: ByteArray = privateKey.clone()
    private var cipherState: CipherState? = null

    // Remember the exception we threw so we can throw it again if the user keeps trying to use the stream.
    private var handshakeFailure: IOException? = null

    /**
     * Reads sufficient data to return the user-provided associated/authenticated data from the handshake, or
     * null if none was found.
     */
    var associatedData: ByteArray? = null
        private set
        @Throws(IOException::class) get() {
            maybeHandshake()
            return field
        }

    /**
     * Returns the authenticated public key of the sender. This may be useful to understand who sent you the
     * data, if you know the sender's possible public keys in advance.
     */
    var senderPublicKey: ByteArray? = null
        private set
        @Throws(IOException::class) get() {
            maybeHandshake()
            return field
        }

    private val buffer = ByteArray(Noise.MAX_PACKET_LEN) // Reused to hold encrypted packets.
    private val currentDecryptedBuffer = ByteArray(Noise.MAX_PACKET_LEN) // Current decrypted packet.
    private var currentPos = 0 // How far through the decrypted packet we got.
    private var currentBufferLen = 0 // Real length of data in currentDecryptedBuffer.

    override fun markSupported(): Boolean {
        return false
    }

    @Throws(IOException::class)
    override fun read(): Int {
        maybeHandshake()
        if (currentPos == currentBufferLen) {
            // We reached the end of the current in memory decrypted packet so read another from the stream.
            startNextPacket()
        }
        return if (currentPos == -1)
            -1             // We reached the terminator packet and shouldn't read further.
        else
            currentDecryptedBuffer[currentPos++].toInt() and 0xFF
    }

    @Throws(IOException::class)
    private fun startNextPacket() {
        // Read the length, which includes the MAC tag.
        val cipherState = cipherState!!
        val input = `in`
        val packetLen1 = input.read().also { if (it == -1) error("Truncated stream in header") }
        val packetLen2 = input.read().also { if (it == -1) error("Truncated stream in header") }
        val packetLen: Int = (packetLen1 shl 8) or packetLen2
        if (packetLen < cipherState.macLength)
            error("Packet length $packetLen is less than MAC length ${cipherState.macLength}")

        // Swallow the next packet, blocking until we got it.
        var cursor = 0
        while (cursor < packetLen) {
            val c = input.read(buffer, cursor, packetLen - cursor)
            if (c == -1) {
                // We shouldn't run out of data before reaching the terminator packet, that could be a MITM attack.
                error("Stream ended without a terminator marker. Truncation can imply a MITM attack.")
            }
            cursor += c
        }

        // Now we can decrypt it.
        currentBufferLen = try {
            cipherState.decryptWithAd(null, buffer, 0, currentDecryptedBuffer, 0, packetLen)
        } catch (e: ShortBufferException) {
            // Data was possibly corrupted.
            throw IOException(e)
        } catch (e: BadPaddingException) {
            throw IOException(e)
        }
        // Have we reached the terminator packet?
        currentPos = if (currentBufferLen == 0) -1 else 0
    }

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        var toSkip = n
        var c = 0
        while (toSkip > 0) {
            if (read() == -1) return c.toLong()
            toSkip--
            c++
        }
        return c.toLong()
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        // TODO: When we fully target Java 9+ replace with default InputStream.read(byte[], int, int)
        //       and use Objects.checkFromIndexSize(off, len, b.length);
        if (off + len > b.size || off < 0 || len < 0)
            throw IndexOutOfBoundsException("$off + $len >= ${b.size}")

        if (len == 0) {
            return 0
        }
        var c = read()
        if (c == -1) {
            return -1
        }
        b[off] = c.toByte()
        var i = 1
        try {
            while (i < len) {
                c = read()
                if (c == -1) {
                    break
                }
                b[off + i] = c.toByte()
                i++
            }
        } catch (ee: IOException) {
            // See the spec for InputStream.read(byte[], int, int) to understand this empty catch block.
        }
        return i
    }

    @Throws(IOException::class)
    private fun maybeHandshake() {
        if (cipherState != null) return
        if (handshakeFailure != null) throw handshakeFailure!!
        try {
            val input = `in`!!
            // Read and check the header, construct the handshake based on it.
            val protocolName = readProtocolNameHeader()
            val handshakeLen = input.read() shl 8 or input.read()
            if (handshakeLen <= 0 || handshakeLen > Noise.MAX_PACKET_LEN) error("Bad handshake length $handshakeLen")
            val handshakeBytes = ByteArray(handshakeLen)
            if (input.read(handshakeBytes) < handshakeLen) error("Premature end of stream whilst reading the handshake")
            val handshake: HandshakeState = setupHandshake(protocolName)
            try {
                readHandshake(handshakeLen, handshakeBytes, handshake)
                checkRemotePublicKey(handshake)
                assert(handshake.action == HandshakeState.SPLIT)
                // Setup done, so retrieve the per-message key.
                val split: CipherStatePair = handshake.split()
                split.receiverOnly()
                cipherState = split.receiver
                assert(handshake.action == HandshakeState.COMPLETE)
            } finally {
                handshake.destroy()
            }
        } catch (e: Exception) {
            handshakeFailure = IOException(e)
        } finally {
            Noise.destroy(privateKey)
        }
        if (handshakeFailure != null) throw handshakeFailure!!
    }

    @Throws(NoSuchAlgorithmException::class)
    private fun setupHandshake(protocolName: String): HandshakeState {
        val handshake = HandshakeState(protocolName, HandshakeState.RESPONDER)
        val localKeyPair = handshake.localKeyPair
        localKeyPair.setPrivateKey(privateKey, 0)
        // The prologue ensures the protocol name wasn't tampered with.
        val prologue: ByteArray = computePrologue(protocolName.toByteArray(StandardCharsets.UTF_8))
        handshake.setPrologue(prologue, 0, prologue.size)
        handshake.start()
        assert(handshake.action == HandshakeState.READ_MESSAGE)
        return handshake
    }

    @Throws(ShortBufferException::class, BadPaddingException::class, IOException::class)
    private fun readHandshake(handshakeLen: Int, handshakeBytes: ByteArray, handshake: HandshakeState) {
        val ad = ByteArray(Noise.MAX_PACKET_LEN)
        val len = handshake.readMessage(handshakeBytes, 0, handshakeLen, ad, 0)
        associatedData = if (len == 0) null else ad.copyOfRange(0, len)
    }

    private fun checkRemotePublicKey(handshake: HandshakeState) {
        // The remote public key might be provided, or might be a dummy (corresponding to a private key of zero).
        // Calculate the dummy key and null out the sender public key if it matches that.
        val pk = handshake.remotePublicKey.publicKey
        val dh = Noise.createDH(handshake.protocolName.split('_')[2])
        val privateKeyOfZero = ByteArray(dh.privateKeyLength)
        dh.setPrivateKey(privateKeyOfZero, 0)
        val anonymousPublicKey = dh.publicKey
        if (!Arrays.equals(pk, anonymousPublicKey))
            senderPublicKey = pk
    }

    @Throws(IOException::class)
    private fun readProtocolNameHeader(): String {
        // A byte followed by that many characters in ASCII.
        val input = `in`!!
        val protocolNameLen = input.read()
        if (protocolNameLen == -1) error("No Noise protocol name header found, corrupted stream most likely.")
        // Read it in and advance the stream.
        val protocolNameBytes = ByteArray(protocolNameLen)
        if (input.read(protocolNameBytes) != protocolNameBytes.size) error("Could not read protocol name")
        val protocolName = String(protocolNameBytes, StandardCharsets.US_ASCII)
        // We only allow the X one-way handshake in Mail. We could also support "N" in which the sender doesn't
        // get to pick a public key, at the saving of some bytes. For simplicity we always use "X" and if the sender
        // doesn't have a key, they just use a key of zero. As zero is a valid value for a private key in Curve25519
        // (all values are valid) this just yields some random looking public key which we recognise as special.
        if (!protocolName.startsWith("Noise_X_")) error("Unsupported Noise protocol name: $protocolName")
        return protocolName
    }

    @Throws(IOException::class)
    private fun error(s: String) {
        throw IOException("$s. Corrupt stream or not Conclave Mail.")
    }
}

private fun computePrologue(protocolNameBytes: ByteArray): ByteArray {
    // We compute the prologue as in the Noise Socket spec (as of August 2019). The actual value doesn't really
    // matter as long as it contains the protocol name, which we need to ensure isn't tampered with. So we
    // use "NoiseSocketInit1" + <negotiation_len> + <negotiation data> which is currently just the protocol name.
    // Note that Noise Socket is like the Noise Protocol itself - it's not a real protocol, it's a recipe for
    // baking protocols. We use it here only to simplify future interop work. It's OK if the spec changes.
    val noiseSocketPrologueHeader = "NoiseSocketInit1".toByteArray(StandardCharsets.UTF_8)
    val prologue = ByteArray(noiseSocketPrologueHeader.size + 2 + protocolNameBytes.size)
    System.arraycopy(noiseSocketPrologueHeader, 0, prologue, 0, noiseSocketPrologueHeader.size)
    prologue[noiseSocketPrologueHeader.size] = (protocolNameBytes.size shr 8).toByte()
    prologue[noiseSocketPrologueHeader.size + 1] = protocolNameBytes.size.toByte()
    System.arraycopy(protocolNameBytes, 0, prologue, noiseSocketPrologueHeader.size + 2, protocolNameBytes.size)
    return prologue
}
