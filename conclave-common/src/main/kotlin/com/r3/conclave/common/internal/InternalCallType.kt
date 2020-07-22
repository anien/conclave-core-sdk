package com.r3.conclave.common.internal

/**
 * Byte used to disambiguate different kinds of messages on the enclave message handler.
 */
enum class InternalCallType {
    /** Top level call */
    CALL,
    /** Bytes returned from [com.r3.conclave.common.enclave.EnclaveCall.invoke]. */
    CALL_RETURN,
    /**
     * On the inbound-to-enclave path, contains a mail to be decrypted. On the outbound path, contains a serialised
     * [MailCommand].
     */
    MAIL_DELIVERY
}