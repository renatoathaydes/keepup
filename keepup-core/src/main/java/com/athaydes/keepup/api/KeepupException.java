package com.athaydes.keepup.api;

import java.util.function.Consumer;

/**
 * Error that may occur during a Keepup update cycle.
 * <p>
 * Applications should handle this exception in the {@link Keepup#onError(Consumer)} callback.
 */
public class KeepupException extends RuntimeException {

    /**
     * Simple error codes describing at a high level what kind of issues may cause a {@link KeepupException}.
     */
    public enum ErrorCode {
        APP_HOME,
        DOWNLOAD,
        LATEST_VERSION_CHECK,
        NO_UPDATE_CALLBACK,
        DONE_CALLBACK,
        UNPACK,
        VERIFY_UPDATE,
        CREATE_UPDATE_SCRIPT,
        CURRENT_NOT_JLINK_APP,
        UPDATE_NOT_JLINK_APP,
        CANNOT_REMOVE_UPDATE_ZIP,
    }

    private final ErrorCode errorCode;

    public KeepupException(ErrorCode errorCode, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }

    public KeepupException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return "KeepupException{" +
                "errorCode=" + errorCode +
                ", cause=" + getCause() +
                ", message=" + getMessage() +
                '}';
    }
}
