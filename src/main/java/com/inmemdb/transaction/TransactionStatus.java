package com.inmemdb.transaction;

/**
 * Represents the status of a transaction
 */
public enum TransactionStatus {
    /**
     * Transaction is active and accepting operations
     */
    ACTIVE,

    /**
     * Transaction has been committed successfully
     */
    COMMITTED,

    /**
     * Transaction has been rolled back
     */
    ROLLED_BACK,

    /**
     * Transaction failed during commit
     */
    FAILED
}
