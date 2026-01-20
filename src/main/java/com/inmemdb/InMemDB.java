package com.inmemdb;

import com.inmemdb.storage.InMemoryKeyValueStore;
import com.inmemdb.storage.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * InMemDB - In-Memory Database
 * Main entry point for the in-memory database
 */
public class InMemDB {
    private static final Logger logger = LoggerFactory.getLogger(InMemDB.class);

    private final KeyValueStore<String, Object> store;

    public InMemDB() {
        this.store = new InMemoryKeyValueStore<>();
        logger.info("InMemDB initialized with KeyValueStore");
    }

    /**
     * Get the underlying key-value store
     * @return the KeyValueStore instance
     */
    public KeyValueStore<String, Object> getStore() {
        return store;
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
        logger.info("InMemDB running...");
        System.out.println("InMemDB is ready!");
        System.out.println("Key-Value Store initialized and ready for operations.");
    }

    /**
     * Run a demo of the database capabilities
     */
    public void runDemo() {
        System.out.println("\n=== InMemDB Demo ===\n");

        // String values
        set("username", "john_doe");
        set("email", "john@example.com");

        // Integer values
        set("age", 30);
        set("login_count", 5);

        // Display values
        System.out.println("Username: " + get("username").orElse("N/A"));
        System.out.println("Email: " + get("email").orElse("N/A"));
        System.out.println("Age: " + get("age").orElse("N/A"));

        // Update value
        store.update("age", 31);
        System.out.println("Updated Age: " + get("age").orElse("N/A"));

        // Store size
        System.out.println("\nTotal entries: " + store.size());

        // List all keys
        System.out.println("All keys: " + store.keys());

        // Delete a key
        delete("login_count");
        System.out.println("After deletion, total entries: " + store.size());

        System.out.println("\n=== Demo Complete ===\n");
    }

    public static void main(String[] args) {
        logger.info("Starting InMemDB application");
        InMemDB db = new InMemDB();
        db.run();

        // Run demo
        db.runDemo();
    }
}
