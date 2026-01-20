package com.inmemdb.transaction;

import com.inmemdb.storage.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Transactional wrapper around a KeyValueStore
 * Provides isolated view of data within a transaction
 */
class TransactionalStore implements KeyValueStore<String, Object> {
    private static final Logger logger = LoggerFactory.getLogger(TransactionalStore.class);

    private final String transactionId;
    private final KeyValueStore<String, Object> underlyingStore;
    private final Map<String, Object> workspace;
    private final Set<String> deletedKeys;
    private final TransactionStatusProvider statusProvider;

    public TransactionalStore(String transactionId,
                              KeyValueStore<String, Object> underlyingStore,
                              Map<String, Object> workspace,
                              Set<String> deletedKeys,
                              TransactionStatusProvider statusProvider) {
        this.transactionId = transactionId;
        this.underlyingStore = underlyingStore;
        this.workspace = workspace;
        this.deletedKeys = deletedKeys;
        this.statusProvider = statusProvider;
    }

    @Override
    public Object put(String key, Object value) {
        ensureActive();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        // Deep copy to ensure isolation
        Object copiedValue = ValueCopier.deepCopy(value);
        Object previous = workspace.put(key, copiedValue);
        deletedKeys.remove(key);
        logger.debug("TX-{}: put key={}", transactionId, key);
        return previous;
    }

    @Override
    public Optional<Object> get(String key) {
        ensureActive();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        // Check if deleted in this transaction
        if (deletedKeys.contains(key)) {
            return Optional.empty();
        }

        // Check workspace first
        if (workspace.containsKey(key)) {
            Object value = workspace.get(key);
            return Optional.ofNullable(ValueCopier.deepCopy(value));
        }

        // Read from underlying store
        Optional<Object> storeValue = underlyingStore.get(key);
        return storeValue.map(ValueCopier::deepCopy);
    }

    @Override
    public boolean update(String key, Object value) {
        ensureActive();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        // Check if key exists (in workspace or store)
        boolean exists = workspace.containsKey(key) ||
                        (!deletedKeys.contains(key) && underlyingStore.containsKey(key));

        if (exists) {
            Object copiedValue = ValueCopier.deepCopy(value);
            workspace.put(key, copiedValue);
            deletedKeys.remove(key);
            logger.debug("TX-{}: update key={}", transactionId, key);
            return true;
        }

        return false;
    }

    @Override
    public Object delete(String key) {
        ensureActive();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        deletedKeys.add(key);
        Object removed = workspace.remove(key);

        // If not in workspace, check underlying store
        if (removed == null && underlyingStore.containsKey(key)) {
            removed = underlyingStore.get(key).orElse(null);
        }

        logger.debug("TX-{}: delete key={}", transactionId, key);
        return removed;
    }

    @Override
    public boolean containsKey(String key) {
        ensureActive();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        if (deletedKeys.contains(key)) {
            return false;
        }

        return workspace.containsKey(key) || underlyingStore.containsKey(key);
    }

    @Override
    public int size() {
        ensureActive();
        // This is an approximate size within the transaction
        Set<String> allKeys = new HashSet<>(underlyingStore.keys());
        allKeys.addAll(workspace.keySet());
        allKeys.removeAll(deletedKeys);
        return allKeys.size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void clear() {
        ensureActive();
        // Mark all existing keys as deleted
        deletedKeys.addAll(underlyingStore.keys());
        // Clear workspace
        workspace.clear();
        logger.debug("TX-{}: clear", transactionId);
    }

    @Override
    public Set<String> keys() {
        ensureActive();
        Set<String> allKeys = new HashSet<>(underlyingStore.keys());
        allKeys.addAll(workspace.keySet());
        allKeys.removeAll(deletedKeys);
        return allKeys;
    }

    @Override
    public Map<String, Object> getAll() {
        ensureActive();
        Map<String, Object> result = new HashMap<>();

        // Start with underlying store
        for (String key : underlyingStore.keys()) {
            if (!deletedKeys.contains(key)) {
                underlyingStore.get(key).ifPresent(v -> result.put(key, ValueCopier.deepCopy(v)));
            }
        }

        // Override with workspace values
        for (Map.Entry<String, Object> entry : workspace.entrySet()) {
            result.put(entry.getKey(), ValueCopier.deepCopy(entry.getValue()));
        }

        return result;
    }

    @Override
    public boolean compareAndSwap(String key, Object oldValue, Object newValue) {
        ensureActive();
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (newValue == null) {
            throw new IllegalArgumentException("New value cannot be null");
        }

        Optional<Object> current = get(key);
        if (current.isPresent() && Objects.equals(current.get(), oldValue)) {
            put(key, newValue);
            return true;
        }
        return false;
    }

    @Override
    public Number incrementAndGet(String key, long delta) {
        ensureActive();
        // Not supported in transactions - would require locking
        throw new UnsupportedOperationException("Atomic operations not supported in transactions");
    }

    @Override
    public Number decrementAndGet(String key, long delta) {
        ensureActive();
        // Not supported in transactions - would require locking
        throw new UnsupportedOperationException("Atomic operations not supported in transactions");
    }

    private void ensureActive() {
        if (!statusProvider.isActive()) {
            throw new IllegalStateException("Transaction " + transactionId + " is not active");
        }
    }

    /**
     * Interface for checking transaction status
     */
    interface TransactionStatusProvider {
        boolean isActive();
    }
}
