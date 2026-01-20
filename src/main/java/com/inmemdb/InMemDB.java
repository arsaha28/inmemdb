package com.inmemdb;

import com.inmemdb.storage.InMemoryKeyValueStore;
import com.inmemdb.storage.KeyValueStore;
import com.inmemdb.transaction.Transaction;
import com.inmemdb.transaction.TransactionException;
import com.inmemdb.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * InMemDB - In-Memory Database
 * Main entry point for the in-memory database with transaction support
 */
public class InMemDB {
    private static final Logger logger = LoggerFactory.getLogger(InMemDB.class);

    private final KeyValueStore<String, Object> store;
    private final TransactionManager transactionManager;

    public InMemDB() {
        this.store = new InMemoryKeyValueStore<>();
        this.transactionManager = new TransactionManager(store);
        logger.info("InMemDB initialized with KeyValueStore and TransactionManager");
    }

    /**
     * Get the underlying key-value store
     * @return the KeyValueStore instance
     */
    public KeyValueStore<String, Object> getStore() {
        return store;
    }

    /**
     * Get the transaction manager
     * @return the TransactionManager instance
     */
    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    /**
     * Begin a new transaction
     * @return the new transaction
     */
    public Transaction beginTransaction() {
        return transactionManager.beginTransaction();
    }

    /**
     * Execute an operation within a transaction with automatic commit/rollback
     * @param operation the operation to execute
     * @param <T> the return type
     * @return the result of the operation
     * @throws TransactionException if the transaction fails
     */
    public <T> T executeTransaction(TransactionManager.TransactionOperation<T> operation) throws TransactionException {
        return transactionManager.executeTransaction(operation);
    }

    /**
     * Execute a void operation within a transaction with automatic commit/rollback
     * @param operation the operation to execute
     * @throws TransactionException if the transaction fails
     */
    public void executeTransaction(TransactionManager.VoidTransactionOperation operation) throws TransactionException {
        transactionManager.executeTransaction(operation);
    }

    /**
     * Store a value
     * @param key the key
     * @param value the value
     */
    public void set(String key, Object value) {
        store.put(key, value);
        logger.info("Set key: {} with value type: {}", key, value.getClass().getSimpleName());
    }

    /**
     * Retrieve a value
     * @param key the key
     * @return the value if present
     */
    public Optional<Object> get(String key) {
        return store.get(key);
    }

    /**
     * Delete a value
     * @param key the key
     * @return the deleted value
     */
    public Object delete(String key) {
        Object deleted = store.delete(key);
        logger.info("Deleted key: {}", key);
        return deleted;
    }

    public void run() {
        logger.info("InMemDB running - Key-Value Store initialized and ready for operations");
    }

    /**
     * Run a demo of the database capabilities
     */
    public void runDemo() {
        logger.info("=== InMemDB Demo Starting ===");

        // Basic CRUD operations
        set("username", "john_doe");
        set("email", "john@example.com");
        set("age", 30);
        set("login_count", 5);

        logger.info("Username: {}", get("username").orElse("N/A"));
        logger.info("Email: {}", get("email").orElse("N/A"));
        logger.info("Age: {}", get("age").orElse("N/A"));

        // Update value
        store.update("age", 31);
        logger.info("Updated Age: {}", get("age").orElse("N/A"));

        // Store size
        logger.info("Total entries: {}", store.size());
        logger.info("All keys: {}", store.keys());

        // Delete a key
        delete("login_count");
        logger.info("After deletion, total entries: {}", store.size());

        // Atomic operations
        logger.info("--- Atomic Operations Demo ---");
        store.put("counter", 0);
        store.incrementAndGet("counter", 10);
        logger.info("Counter after increment: {}", get("counter").orElse(0));

        store.decrementAndGet("counter", 3);
        logger.info("Counter after decrement: {}", get("counter").orElse(0));

        // Compare and swap
        boolean swapped = store.compareAndSwap("username", "john_doe", "jane_doe");
        logger.info("CAS successful: {}, new username: {}", swapped, get("username").orElse("N/A"));

        // Transaction demo
        logger.info("--- Transaction Demo ---");
        try {
            executeTransaction(tx -> {
                tx.getStore().put("account1", 1000);
                tx.getStore().put("account2", 500);
                logger.info("Transaction: Created accounts");
            });
            logger.info("Account1 balance: {}", get("account1").orElse(0));
            logger.info("Account2 balance: {}", get("account2").orElse(0));

            // Transaction with rollback on error
            try {
                executeTransaction(tx -> {
                    tx.getStore().put("account1", 900);
                    tx.getStore().put("account2", 600);
                    throw new RuntimeException("Simulated error");
                });
            } catch (TransactionException e) {
                logger.info("Transaction rolled back due to error");
            }

            logger.info("Account1 balance after rollback: {}", get("account1").orElse(0));
            logger.info("Account2 balance after rollback: {}", get("account2").orElse(0));

        } catch (TransactionException e) {
            logger.error("Transaction failed", e);
        }

        logger.info("=== InMemDB Demo Complete ===");
    }

    public static void main(String[] args) {
        logger.info("Starting InMemDB application");
        InMemDB db = new InMemDB();
        db.run();

        // Run demo
        db.runDemo();
    }
}
