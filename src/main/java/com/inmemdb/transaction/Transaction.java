package com.inmemdb.transaction;

import com.inmemdb.storage.KeyValueStore;

/**
 * Interface representing a database transaction
 * Manages transaction lifecycle - data operations performed through getStore()
 */
public interface Transaction {

    /**
     * Get the transaction ID
     * @return the unique transaction ID
     */
    String getId();

    /**
     * Get the transaction status
     * @return the current status
     */
    TransactionStatus getStatus();

    /**
     * Get a transactional view of the store
     * All operations on this store are part of the transaction
     * @return transactional KeyValueStore instance
     */
    KeyValueStore<String, Object> getStore();

    /**
     * Commit the transaction
     * Applies all changes to the underlying store
     * @throws TransactionException if commit fails
     */
    void commit() throws TransactionException;

    /**
     * Rollback the transaction
     * Discards all changes
     */
    void rollback();

    /**
     * Check if the transaction is active
     * @return true if active, false otherwise
     */
    boolean isActive();
}
