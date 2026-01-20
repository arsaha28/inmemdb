package com.inmemdb.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory implementation of KeyValueStore
 * Uses ConcurrentHashMap for concurrent access support
 */
public class InMemoryKeyValueStore<K, V> implements KeyValueStore<K, V> {
    private static final Logger logger = LoggerFactory.getLogger(InMemoryKeyValueStore.class);

    private final ConcurrentHashMap<K, V> store;

    public InMemoryKeyValueStore() {
        this.store = new ConcurrentHashMap<>();
        logger.debug("InMemoryKeyValueStore initialized");
    }

    @Override
    public V put(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        V previousValue = store.put(key, value);
        logger.debug("Put key: {}, value: {}, previous: {}", key, value, previousValue);
        return previousValue;
    }

    @Override
    public Optional<V> get(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        V value = store.get(key);
        logger.debug("Get key: {}, value: {}", key, value);
        return Optional.ofNullable(value);
    }

    @Override
    public boolean update(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        V previousValue = store.replace(key, value);
        boolean updated = previousValue != null;
        logger.debug("Update key: {}, value: {}, success: {}", key, value, updated);
        return updated;
    }

    @Override
    public V delete(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        V removedValue = store.remove(key);
        logger.debug("Delete key: {}, removed value: {}", key, removedValue);
        return removedValue;
    }

    @Override
    public boolean containsKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        return store.containsKey(key);
    }

    @Override
    public int size() {
        return store.size();
    }

    @Override
    public boolean isEmpty() {
        return store.isEmpty();
    }

    @Override
    public void clear() {
        int previousSize = store.size();
        store.clear();
        logger.debug("Clear store, removed {} entries", previousSize);
    }

    @Override
    public Set<K> keys() {
        return new HashSet<>(store.keySet());
    }

    @Override
    public Map<K, V> getAll() {
        return new HashMap<>(store);
    }

    /**
     * Get a value with a default if key doesn't exist
     * @param key the key
     * @param defaultValue the default value to return if key not found
     * @return the value or default
     */
    public V getOrDefault(K key, V defaultValue) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        return store.getOrDefault(key, defaultValue);
    }

    /**
     * Put a value only if the key doesn't exist
     * @param key the key
     * @param value the value
     * @return the previous value if key existed, null otherwise
     */
    public V putIfAbsent(K key, V value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        V previousValue = store.putIfAbsent(key, value);
        logger.debug("PutIfAbsent key: {}, value: {}, previous: {}", key, value, previousValue);
        return previousValue;
    }
}
