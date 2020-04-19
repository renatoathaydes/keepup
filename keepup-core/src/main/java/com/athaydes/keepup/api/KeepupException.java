package com.athaydes.keepup.api;

public class KeepupException extends RuntimeException {
    public enum ErrorCode {
        DOWNLOAD,
        LATEST_VERSION_CHECK,
        NO_UPDATE_CALLBACK,
        DONE_CALLBACK,
        UNPACK,
        VERIFY_UPDATE,
        CREATE_UPDATE_SCRIPT,
        CURRENT_NOT_JLINK_APP,
        UPGRADE_NOT_JLINK_APP,
        CANNOT_REMOVE_UPGRADE_ZIP,
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
