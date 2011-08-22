package com.mozilla.grouperfish.batch;

public class RetryException extends Exception {

    private static final long serialVersionUID = 0;

    public RetryException(final String message) {
        super(message);
    }

    public RetryException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
