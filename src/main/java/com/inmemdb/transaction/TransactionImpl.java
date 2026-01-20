package com.inmemdb.transaction;

import com.inmemdb.storage.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of Transaction with MVCC-like isolation
 * Maintains a local workspace for changes until commit
 * Uses deep copying to ensure complete isolation from storage layer
 */
class TransactionImpl implements Transaction {
    private static final Logger logger = LoggerFactory.getLogger(TransactionImpl.class);

    private final String id;
    private final KeyValueStore<String, Object> store;
    private final Map<String, Object> workspace;
    private final Set<String> deletedKeys;
    private TransactionStatus status;

    public TransactionImpl(String id, KeyValueStore<String, Object> store) {
        this.id = id;
        this.store = store;
        this.workspace = new ConcurrentHashMap<>();
        this.deletedKeys = ConcurrentHashMap.newKeySet();
        this.status = TransactionStatus.ACTIVE;
        logger.debug("Transaction {} created with isolated workspace", id);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public TransactionStatus getStatus() {
        return status;
    }

    @Override
    public void put(String key, Object value) {
        ensureActive();
        // Deep copy the value to ensure isolation from external modifications
        Object copiedValue = ValueCopier.deepCopy(value);
        workspace.put(key, copiedValue);
        deletedKeys.remove(key);
        logger.debug("Transaction {}: put key={} (deep copied)", id, key);
    }

    @Override
    public Optional<Object> get(String key) {
        ensureActive();

        // Check if deleted in this transaction
        if (deletedKeys.contains(key)) {
            return Optional.empty();
        }

        // Check workspace first (return a copy to prevent external modifications)
        if (workspace.containsKey(key)) {
            Object value = workspace.get(key);
            return Optional.ofNullable(ValueCopier.deepCopy(value));
        }

        // Read from store (copy to ensure transaction isolation)
        Optional<Object> storeValue = store.get(key);
        return storeValue.map(ValueCopier::deepCopy);
    }

    @Override
    public void delete(String key) {
        ensureActive();
        deletedKeys.add(key);
        workspace.remove(key);
        logger.debug("Transaction {}: delete key={}", id, key);
    }

    @Override
    public synchronized void commit() throws TransactionException {
        ensureActive();

        try {
            logger.debug("Transaction {}: committing {} changes, {} deletions",
                    id, workspace.size(), deletedKeys.size());

            // Apply deletions
            for (String key : deletedKeys) {
                store.delete(key);
            }

            // Apply updates/inserts (workspace contains deep copies, safe to commit)
            for (Map.Entry<String, Object> entry : workspace.entrySet()) {
                store.put(entry.getKey(), entry.getValue());
            }

            status = TransactionStatus.COMMITTED;
            logger.info("Transaction {} committed successfully", id);

        } catch (Exception e) {
            status = TransactionStatus.FAILED;
            logger.error("Transaction {} commit failed", id, e);
            throw new TransactionException("Failed to commit transaction: " + id, e);
        }
    }

    @Override
    public synchronized void rollback() {
        if (status == TransactionStatus.ACTIVE) {
            workspace.clear();
            deletedKeys.clear();
            status = TransactionStatus.ROLLED_BACK;
            logger.info("Transaction {} rolled back", id);
        }
    }

    @Override
    public boolean isActive() {
        return status == TransactionStatus.ACTIVE;
    }

    private void ensureActive() {
        if (!isActive()) {
            throw new IllegalStateException("Transaction " + id + " is not active (status: " + status + ")");
        }
    }
}
