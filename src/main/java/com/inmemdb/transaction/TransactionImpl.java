package com.inmemdb.transaction;

import com.inmemdb.storage.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of Transaction with MVCC-like isolation
 * Delegates data operations to TransactionalStore, manages only lifecycle
 */
class TransactionImpl implements Transaction {
    private static final Logger logger = LoggerFactory.getLogger(TransactionImpl.class);

    private final String id;
    private final KeyValueStore<String, Object> underlyingStore;
    private final Map<String, Object> workspace;
    private final Set<String> deletedKeys;
    private final TransactionalStore transactionalStore;
    private TransactionStatus status;

    public TransactionImpl(String id, KeyValueStore<String, Object> underlyingStore) {
        this.id = id;
        this.underlyingStore = underlyingStore;
        this.workspace = new ConcurrentHashMap<>();
        this.deletedKeys = ConcurrentHashMap.newKeySet();
        this.status = TransactionStatus.ACTIVE;
        this.transactionalStore = new TransactionalStore(
                id,
                underlyingStore,
                workspace,
                deletedKeys,
                this::isActive
        );
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
    public KeyValueStore<String, Object> getStore() {
        return transactionalStore;
    }

    @Override
    public synchronized void commit() throws TransactionException {
        ensureActive();

        try {
            logger.debug("Transaction {}: committing {} changes, {} deletions",
                    id, workspace.size(), deletedKeys.size());

            // Apply deletions
            for (String key : deletedKeys) {
                underlyingStore.delete(key);
            }

            // Apply updates/inserts (workspace contains deep copies, safe to commit)
            for (Map.Entry<String, Object> entry : workspace.entrySet()) {
                underlyingStore.put(entry.getKey(), entry.getValue());
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
