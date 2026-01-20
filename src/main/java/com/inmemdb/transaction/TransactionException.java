package com.inmemdb.transaction;

/**
 * Exception thrown when transaction operations fail
 */
public class TransactionException extends Exception {

    public TransactionException(String message) {
        super(message);
    }

    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
