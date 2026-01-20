package com.inmemdb.transaction;

import com.inmemdb.storage.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages database transactions with thread safety
 * Provides transaction lifecycle management and isolation
 */
public class TransactionManager {
    private static final Logger logger = LoggerFactory.getLogger(TransactionManager.class);

    private final KeyValueStore<String, Object> store;
    private final Map<String, Transaction> activeTransactions;
    private final ReadWriteLock globalLock;
    private final AtomicLong transactionCounter;

    public TransactionManager(KeyValueStore<String, Object> store) {
        this.store = store;
        this.activeTransactions = new ConcurrentHashMap<>();
        this.globalLock = new ReentrantReadWriteLock();
        this.transactionCounter = new AtomicLong(0);
        logger.info("TransactionManager initialized");
    }

    /**
     * Begin a new transaction
     * @return the new transaction
     */
    public Transaction beginTransaction() {
        String txId = generateTransactionId();
        Transaction transaction = new TransactionImpl(txId, store);
        activeTransactions.put(txId, transaction);
        logger.debug("Transaction {} started", txId);
        return transaction;
    }

    /**
     * Execute a transaction with automatic commit/rollback
     * @param operation the operation to execute within the transaction
     * @param <T> the return type
     * @return the result of the operation
     * @throws TransactionException if the transaction fails
     */
    public <T> T executeTransaction(TransactionOperation<T> operation) throws TransactionException {
        Transaction tx = beginTransaction();
        try {
            T result = operation.execute(tx);
            tx.commit();
            return result;
        } catch (Exception e) {
            tx.rollback();
            logger.error("Transaction {} failed and rolled back", tx.getId(), e);
            throw new TransactionException("Transaction failed: " + e.getMessage(), e);
        } finally {
            activeTransactions.remove(tx.getId());
        }
    }

    /**
     * Execute a void transaction with automatic commit/rollback
     * @param operation the operation to execute
     * @throws TransactionException if the transaction fails
     */
    public void executeTransaction(VoidTransactionOperation operation) throws TransactionException {
        Transaction tx = beginTransaction();
        try {
            operation.execute(tx);
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            logger.error("Transaction {} failed and rolled back", tx.getId(), e);
            throw new TransactionException("Transaction failed: " + e.getMessage(), e);
        } finally {
            activeTransactions.remove(tx.getId());
        }
    }

    /**
     * Get an active transaction by ID
     * @param transactionId the transaction ID
     * @return the transaction if active
     */
    public Transaction getTransaction(String transactionId) {
        return activeTransactions.get(transactionId);
    }

    /**
     * Get the number of active transactions
     * @return the count of active transactions
     */
    public int getActiveTransactionCount() {
        return activeTransactions.size();
    }

    /**
     * Acquire a read lock for concurrent read operations
     */
    public void acquireReadLock() {
        globalLock.readLock().lock();
    }

    /**
     * Release the read lock
     */
    public void releaseReadLock() {
        globalLock.readLock().unlock();
    }

    /**
     * Acquire a write lock for exclusive write operations
     */
    public void acquireWriteLock() {
        globalLock.writeLock().lock();
    }

    /**
     * Release the write lock
     */
    public void releaseWriteLock() {
        globalLock.writeLock().unlock();
    }

    private String generateTransactionId() {
        long count = transactionCounter.incrementAndGet();
        return "TX-" + count + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Functional interface for transaction operations that return a value
     * @param <T> the return type
     */
    @FunctionalInterface
    public interface TransactionOperation<T> {
        T execute(Transaction tx) throws Exception;
    }

    /**
     * Functional interface for void transaction operations
     */
    @FunctionalInterface
    public interface VoidTransactionOperation {
        void execute(Transaction tx) throws Exception;
    }
}
