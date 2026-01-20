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

    @Override
    public boolean compareAndSwap(K key, V oldValue, V newValue) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (newValue == null) {
            throw new IllegalArgumentException("New value cannot be null");
        }

        boolean success = store.replace(key, oldValue, newValue);
        logger.debug("CompareAndSwap key: {}, oldValue: {}, newValue: {}, success: {}",
                key, oldValue, newValue, success);
        return success;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Number incrementAndGet(K key, long delta) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        Number result = store.compute(key, (k, v) -> {
            if (v == null) {
                return (V) Long.valueOf(delta);
            }
            if (!(v instanceof Number)) {
                throw new IllegalArgumentException("Value is not a Number: " + v.getClass());
            }

            Number num = (Number) v;
            if (num instanceof Integer) {
                return (V) Integer.valueOf(num.intValue() + (int) delta);
            } else if (num instanceof Long) {
                return (V) Long.valueOf(num.longValue() + delta);
            } else if (num instanceof Double) {
                return (V) Double.valueOf(num.doubleValue() + delta);
            } else if (num instanceof Float) {
                return (V) Float.valueOf(num.floatValue() + delta);
            } else {
                return (V) Long.valueOf(num.longValue() + delta);
            }
        });

        logger.debug("IncrementAndGet key: {}, delta: {}, result: {}", key, delta, result);
        return result;
    }

    @Override
    public Number decrementAndGet(K key, long delta) {
        return incrementAndGet(key, -delta);
    }
}
