package com.inmemdb.storage;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Interface for key-value storage operations
 * Provides basic CRUD operations for in-memory data storage
 */
public interface KeyValueStore<K, V> {

    /**
     * Store a key-value pair
     * @param key the key
     * @param value the value to store
     * @return the previous value associated with key, or null if there was no mapping
     */
    V put(K key, V value);

    /**
     * Retrieve a value by key
     * @param key the key
     * @return Optional containing the value if present, empty otherwise
     */
    Optional<V> get(K key);

    /**
     * Update an existing key with a new value
     * @param key the key
     * @param value the new value
     * @return true if the key existed and was updated, false otherwise
     */
    boolean update(K key, V value);

    /**
     * Delete a key-value pair
     * @param key the key to delete
     * @return the value that was removed, or null if key didn't exist
     */
    V delete(K key);

    /**
     * Check if a key exists
     * @param key the key to check
     * @return true if the key exists, false otherwise
     */
    boolean containsKey(K key);

    /**
     * Get the number of key-value pairs
     * @return the size of the store
     */
    int size();

    /**
     * Check if the store is empty
     * @return true if empty, false otherwise
     */
    boolean isEmpty();

    /**
     * Clear all key-value pairs
     */
    void clear();

    /**
     * Get all keys
     * @return a set of all keys
     */
    Set<K> keys();

    /**
     * Get all key-value pairs
     * @return a map of all key-value pairs
     */
    Map<K, V> getAll();

    /**
     * Atomically replace the value for a key only if currently mapped to a given value
     * @param key the key
     * @param oldValue the expected current value
     * @param newValue the new value
     * @return true if the value was replaced
     */
    boolean compareAndSwap(K key, V oldValue, V newValue);

    /**
     * Atomically increment a numeric value
     * Only works if the value is a Number type
     * @param key the key
     * @param delta the amount to increment by
     * @return the new value after increment
     * @throws IllegalArgumentException if value is not a Number
     */
    Number incrementAndGet(K key, long delta);

    /**
     * Atomically decrement a numeric value
     * Only works if the value is a Number type
     * @param key the key
     * @param delta the amount to decrement by
     * @return the new value after decrement
     * @throws IllegalArgumentException if value is not a Number
     */
    Number decrementAndGet(K key, long delta);
}
