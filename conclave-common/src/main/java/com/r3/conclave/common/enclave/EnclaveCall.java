package com.r3.conclave.common.enclave;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A transformation of byte array to optional result byte array. EnclaveCall
 * may be implemented on {@code Enclave} to allow it to receive byte arrays from the
 * untrusted host and respond, but it may also be implemented by temporary
 * closures to allow a form of virtual stack to be created that threads between
 * the untrusted host and the enclave.
 */
@FunctionalInterface
public interface EnclaveCall {
    @Nullable
    byte[] invoke(@NotNull byte[] bytes);
}
