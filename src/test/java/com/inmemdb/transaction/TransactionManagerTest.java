package com.inmemdb.transaction;

import com.inmemdb.storage.InMemoryKeyValueStore;
import com.inmemdb.storage.KeyValueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TransactionManager
 */
@DisplayName("TransactionManager Tests")
public class TransactionManagerTest {
    private KeyValueStore<String, Object> store;
    private TransactionManager txManager;

    @BeforeEach
    public void setUp() {
        store = new InMemoryKeyValueStore<>();
        txManager = new TransactionManager(store);
    }

    @Test
    @DisplayName("Should begin transaction")
    public void testBeginTransaction() {
        Transaction tx = txManager.beginTransaction();

        assertNotNull(tx);
        assertNotNull(tx.getId());
        assertEquals(TransactionStatus.ACTIVE, tx.getStatus());
        assertTrue(tx.isActive());
    }

    @Test
    @DisplayName("Should commit transaction successfully")
    public void testCommitTransaction() throws TransactionException {
        Transaction tx = txManager.beginTransaction();
        tx.put("key1", "value1");
        tx.put("key2", "value2");

        tx.commit();

        assertEquals(TransactionStatus.COMMITTED, tx.getStatus());
        assertEquals("value1", store.get("key1").orElse(null));
        assertEquals("value2", store.get("key2").orElse(null));
    }

    @Test
    @DisplayName("Should rollback transaction")
    public void testRollbackTransaction() {
        Transaction tx = txManager.beginTransaction();
        tx.put("key1", "value1");
        tx.put("key2", "value2");

        tx.rollback();

        assertEquals(TransactionStatus.ROLLED_BACK, tx.getStatus());
        assertFalse(store.containsKey("key1"));
        assertFalse(store.containsKey("key2"));
    }

    @Test
    @DisplayName("Should isolate transaction changes before commit")
    public void testTransactionIsolation() {
        store.put("key1", "original");

        Transaction tx = txManager.beginTransaction();
        tx.put("key1", "modified");

        // Store should still have original value
        assertEquals("original", store.get("key1").orElse(null));

        // Transaction should see modified value
        assertEquals("modified", tx.get("key1").orElse(null));
    }

    @Test
    @DisplayName("Should handle transaction delete")
    public void testTransactionDelete() throws TransactionException {
        store.put("key1", "value1");

        Transaction tx = txManager.beginTransaction();
        tx.delete("key1");

        // Store should still have the value before commit
        assertTrue(store.containsKey("key1"));

        // Transaction should not see the deleted key
        assertFalse(tx.get("key1").isPresent());

        tx.commit();

        // After commit, store should not have the key
        assertFalse(store.containsKey("key1"));
    }

    @Test
    @DisplayName("Should execute transaction with automatic commit")
    public void testExecuteTransaction() throws TransactionException {
        String result = txManager.executeTransaction(tx -> {
            tx.put("key1", "value1");
            tx.put("key2", "value2");
            return "success";
        });

        assertEquals("success", result);
        assertEquals("value1", store.get("key1").orElse(null));
        assertEquals("value2", store.get("key2").orElse(null));
    }

    @Test
    @DisplayName("Should rollback on exception during executeTransaction")
    public void testExecuteTransactionWithException() {
        assertThrows(TransactionException.class, () -> {
            txManager.executeTransaction(tx -> {
                tx.put("key1", "value1");
                throw new RuntimeException("Test exception");
            });
        });

        // Changes should be rolled back
        assertFalse(store.containsKey("key1"));
    }

    @Test
    @DisplayName("Should execute void transaction")
    public void testExecuteVoidTransaction() throws TransactionException {
        txManager.executeTransaction((TransactionManager.VoidTransactionOperation) tx -> {
            tx.put("key1", "value1");
            tx.put("key2", "value2");
        });

        assertEquals("value1", store.get("key1").orElse(null));
        assertEquals("value2", store.get("key2").orElse(null));
    }

    @Test
    @DisplayName("Should track active transactions")
    public void testActiveTransactionCount() {
        assertEquals(0, txManager.getActiveTransactionCount());

        Transaction tx1 = txManager.beginTransaction();
        assertEquals(1, txManager.getActiveTransactionCount());

        Transaction tx2 = txManager.beginTransaction();
        assertEquals(2, txManager.getActiveTransactionCount());
    }

    @Test
    @DisplayName("Should not allow operations on committed transaction")
    public void testOperationsAfterCommit() throws TransactionException {
        Transaction tx = txManager.beginTransaction();
        tx.put("key1", "value1");
        tx.commit();

        assertThrows(IllegalStateException.class, () -> tx.put("key2", "value2"));
        assertThrows(IllegalStateException.class, () -> tx.get("key2"));
        assertThrows(IllegalStateException.class, () -> tx.delete("key2"));
    }

    @Test
    @DisplayName("Should not allow operations on rolled back transaction")
    public void testOperationsAfterRollback() {
        Transaction tx = txManager.beginTransaction();
        tx.put("key1", "value1");
        tx.rollback();

        assertThrows(IllegalStateException.class, () -> tx.put("key2", "value2"));
        assertThrows(IllegalStateException.class, () -> tx.get("key2"));
        assertThrows(IllegalStateException.class, () -> tx.delete("key2"));
    }

    @Test
    @DisplayName("Should handle concurrent transactions")
    public void testConcurrentTransactions() throws InterruptedException {
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    txManager.executeTransaction((TransactionManager.VoidTransactionOperation) tx -> {
                        tx.put("key" + threadNum, "value" + threadNum);
                    });
                    successCount.incrementAndGet();
                } catch (TransactionException e) {
                    // Transaction failed
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        // All transactions should succeed
        assertEquals(numThreads, successCount.get());

        // All keys should be in the store
        for (int i = 0; i < numThreads; i++) {
            assertEquals("value" + i, store.get("key" + i).orElse(null));
        }
    }

    @Test
    @DisplayName("Should handle read/write locks")
    public void testReadWriteLocks() {
        // Acquire read lock
        txManager.acquireReadLock();
        txManager.releaseReadLock();

        // Acquire write lock
        txManager.acquireWriteLock();
        txManager.releaseWriteLock();

        // Should not throw exceptions
        assertTrue(true);
    }
}
