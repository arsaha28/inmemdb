package com.inmemdb.transaction;

import java.util.Optional;

/**
 * Interface representing a database transaction
 * Provides isolation and atomicity for operations
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
     * Put a value in the transaction context
     * @param key the key
     * @param value the value
     */
    void put(String key, Object value);

    /**
     * Get a value from the transaction context
     * @param key the key
     * @return the value if present
     */
    Optional<Object> get(String key);

    /**
     * Delete a value in the transaction context
     * @param key the key
     */
    void delete(String key);

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
