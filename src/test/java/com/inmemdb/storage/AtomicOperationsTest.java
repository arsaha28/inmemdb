package com.inmemdb.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for atomic operations in KeyValueStore
 */
@DisplayName("Atomic Operations Tests")
public class AtomicOperationsTest {
    private InMemoryKeyValueStore<String, Object> store;

    @BeforeEach
    public void setUp() {
        store = new InMemoryKeyValueStore<>();
    }

    @Test
    @DisplayName("Should compare and swap successfully")
    public void testCompareAndSwapSuccess() {
        store.put("key1", "oldValue");

        boolean result = store.compareAndSwap("key1", "oldValue", "newValue");

        assertTrue(result);
        assertEquals("newValue", store.get("key1").orElse(null));
    }

    @Test
    @DisplayName("Should fail compare and swap with wrong old value")
    public void testCompareAndSwapFailure() {
        store.put("key1", "actualValue");

        boolean result = store.compareAndSwap("key1", "wrongValue", "newValue");

        assertFalse(result);
        assertEquals("actualValue", store.get("key1").orElse(null));
    }

    @Test
    @DisplayName("Should fail compare and swap on non-existent key")
    public void testCompareAndSwapNonExistentKey() {
        boolean result = store.compareAndSwap("nonexistent", "oldValue", "newValue");

        assertFalse(result);
        assertFalse(store.containsKey("nonexistent"));
    }

    @Test
    @DisplayName("Should increment integer value")
    public void testIncrementInteger() {
        store.put("counter", 10);

        Number result = store.incrementAndGet("counter", 5);

        assertEquals(15, result.intValue());
        assertEquals(15, ((Integer) store.get("counter").orElse(0)).intValue());
    }

    @Test
    @DisplayName("Should increment long value")
    public void testIncrementLong() {
        store.put("counter", 100L);

        Number result = store.incrementAndGet("counter", 50);

        assertEquals(150L, result.longValue());
        assertEquals(150L, ((Long) store.get("counter").orElse(0L)).longValue());
    }

    @Test
    @DisplayName("Should increment double value")
    public void testIncrementDouble() {
        store.put("price", 10.5);

        Number result = store.incrementAndGet("price", 5);

        assertEquals(15.5, result.doubleValue(), 0.001);
    }

    @Test
    @DisplayName("Should increment non-existent key starting from zero")
    public void testIncrementNonExistentKey() {
        Number result = store.incrementAndGet("newCounter", 10);

        assertEquals(10L, result.longValue());
        assertEquals(10L, store.get("newCounter").orElse(0L));
    }

    @Test
    @DisplayName("Should decrement integer value")
    public void testDecrementInteger() {
        store.put("counter", 20);

        Number result = store.decrementAndGet("counter", 5);

        assertEquals(15, result.intValue());
        assertEquals(15, ((Integer) store.get("counter").orElse(0)).intValue());
    }

    @Test
    @DisplayName("Should decrement long value")
    public void testDecrementLong() {
        store.put("counter", 100L);

        Number result = store.decrementAndGet("counter", 30);

        assertEquals(70L, result.longValue());
        assertEquals(70L, ((Long) store.get("counter").orElse(0L)).longValue());
    }

    @Test
    @DisplayName("Should throw exception when incrementing non-number")
    public void testIncrementNonNumber() {
        store.put("key1", "string value");

        assertThrows(IllegalArgumentException.class, () -> {
            store.incrementAndGet("key1", 5);
        });
    }

    @Test
    @DisplayName("Should throw exception when decrementing non-number")
    public void testDecrementNonNumber() {
        store.put("key1", "string value");

        assertThrows(IllegalArgumentException.class, () -> {
            store.decrementAndGet("key1", 5);
        });
    }

    @Test
    @DisplayName("Should handle concurrent increments correctly")
    public void testConcurrentIncrements() throws InterruptedException {
        store.put("counter", 0);
        int numThreads = 100;
        int incrementsPerThread = 10;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        store.incrementAndGet("counter", 1);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Should have incremented exactly numThreads * incrementsPerThread times
        int expected = numThreads * incrementsPerThread;
        assertEquals(expected, ((Integer) store.get("counter").orElse(0)).intValue());
    }

    @Test
    @DisplayName("Should handle concurrent compare-and-swap operations")
    public void testConcurrentCompareAndSwap() throws InterruptedException {
        store.put("resource", "initial");
        int numThreads = 10;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        int[] successCount = {0};

        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    boolean success = store.compareAndSwap("resource", "initial", "thread-" + threadNum);
                    if (success) {
                        synchronized (successCount) {
                            successCount[0]++;
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        // Only one thread should succeed in the CAS operation
        assertEquals(1, successCount[0]);

        // Value should be from one of the threads
        String value = (String) store.get("resource").orElse(null);
        assertNotNull(value);
        assertTrue(value.startsWith("thread-"));
    }

    @Test
    @DisplayName("Should handle mixed concurrent operations")
    public void testMixedConcurrentOperations() throws InterruptedException {
        store.put("counter", 0);
        int numThreads = 20;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        // Half the threads increment, half decrement
        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    if (threadNum % 2 == 0) {
                        store.incrementAndGet("counter", 10);
                    } else {
                        store.decrementAndGet("counter", 10);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        // Since half increment and half decrement by the same amount, result should be 0
        assertEquals(0, ((Integer) store.get("counter").orElse(null)).intValue());
    }
}
