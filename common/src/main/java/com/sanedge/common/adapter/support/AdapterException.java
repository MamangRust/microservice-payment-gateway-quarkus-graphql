package com.sanedge.common.adapter.support;

/**
 * Raised by adapters when a remote call fails for a reason other than
 * "resource not found" — the Java counterpart of the Go
 * {@code sharedErrors.ErrFailed("op").WithInternal(err)} pattern.
 */
public class AdapterException extends RuntimeException {

    public AdapterException(String message) {
        super(message);
    }

    public AdapterException(String message, Throwable cause) {
        super(message, cause);
    }
}
