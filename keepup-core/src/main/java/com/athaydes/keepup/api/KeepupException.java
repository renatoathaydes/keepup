package com.athaydes.keepup.api;

public class KeepupException extends RuntimeException {
    public enum ErrorCode {
        DOWNLOAD,
        LATEST_VERSION_CHECK,
        UNPACK,
        UPDATE,
        APP_HOME_NOT_WRITABLE,
        NOT_JLINK_APP,
    }

    private final ErrorCode errorCode;

    public KeepupException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public KeepupException(ErrorCode errorCode, Exception cause) {
        super(cause);
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
                '}';
    }
}
