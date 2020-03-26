package com.r3.conclave.host

import com.r3.conclave.common.SecureHash

/**
 * An EnclaveInfo consists of the hash of an enclave, as computed by the platform specific measurement algorithms,
 * the public key that signed the enclave, a version number chosen by the enclave author and the mode the enclave runs in.
 *
 * You would not normally create your own [EnclaveInfo]. Instead you get one from other platform classes. However, you
 * can create one if you wish to use it as a convenient holder of data.
 *
 * @property codeHash The hash reported in a remote attestation. Note that the hash isn't a simple hash of the enclave
 * file and must be calculated by special platform-specific code, for instance by the Conclave Gradle plugin. This hash
 * is sometimes called a measurement.
 *
 * @property codeSigningKeyHash The hash of the public key that signed the enclave. Usually you'll prefer to check this
 * key rather than the code hash, to allow for upgrades.
 *
 * @property productID A product ID is a 16 bit number that identifies different enclave products/lineages signed by the
 * same key. Enclaves with different product IDs cannot read each others sealed data, so it acts as a form of sandbox or
 * enclave firewall between products produced by the same vendor.
 *
 * @property revocationLevel The revocation level is incremented by an enclave author when a weakness in the enclave code
 * is fixed; doing this will enable clients to avoid connecting to old, compromised enclaves. Revocation levels should
 * not be incremented on every new release, but only when security improvements have been made.
 *
 * Note that this is not the SGX notion of a "CPU SVN", but rather the enclave-specific security version number. We call
 * it revocationLevel here to make it clearer what this actually does.
 *
 * @property enclaveMode The mode the enclave runs in, which is either release, debug or simulation. Only release mode
 * provides enclave security. The other two are only for testing and development purposes.
 */
class EnclaveInfo(
        val codeHash: SecureHash,
        val codeSigningKeyHash: SecureHash,
        val productID: Int,
        val revocationLevel: Int,
        val enclaveMode: EnclaveMode
) {
    override fun equals(other: Any?): Boolean = other === this || other is EnclaveInfo && other.codeHash == codeHash

    override fun hashCode(): Int = codeHash.hashCode()

    override fun toString(): String {
        return "EnclaveInfo(codeHash=$codeHash, codeSigningKeyHash=$codeSigningKeyHash, productID=$productID, " +
                "revocationLevel=$revocationLevel, enclaveMode=$enclaveMode)"
    }
}
