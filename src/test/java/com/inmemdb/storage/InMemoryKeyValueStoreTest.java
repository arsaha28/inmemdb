package com.inmemdb.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InMemoryKeyValueStore
 */
@DisplayName("InMemoryKeyValueStore Tests")
public class InMemoryKeyValueStoreTest {
    private InMemoryKeyValueStore<String, String> store;

    @BeforeEach
    public void setUp() {
        store = new InMemoryKeyValueStore<>();
    }

    @Test
    @DisplayName("Should initialize empty store")
    public void testInitialization() {
        assertTrue(store.isEmpty());
        assertEquals(0, store.size());
    }

    @Test
    @DisplayName("Should put and get value")
    public void testPutAndGet() {
        store.put("key1", "value1");

        Optional<String> result = store.get("key1");
        assertTrue(result.isPresent());
        assertEquals("value1", result.get());
        assertEquals(1, store.size());
    }

    @Test
    @DisplayName("Should return previous value when putting existing key")
    public void testPutReturnsOldValue() {
        store.put("key1", "value1");
        String oldValue = store.put("key1", "value2");

        assertEquals("value1", oldValue);
        assertEquals("value2", store.get("key1").get());
    }

    @Test
    @DisplayName("Should return empty Optional for non-existent key")
    public void testGetNonExistentKey() {
        Optional<String> result = store.get("nonexistent");
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should update existing key")
    public void testUpdate() {
        store.put("key1", "value1");
        boolean updated = store.update("key1", "newValue");

        assertTrue(updated);
        assertEquals("newValue", store.get("key1").get());
    }

    @Test
    @DisplayName("Should not update non-existent key")
    public void testUpdateNonExistentKey() {
        boolean updated = store.update("nonexistent", "value");

        assertFalse(updated);
        assertFalse(store.containsKey("nonexistent"));
    }

    @Test
    @DisplayName("Should delete existing key")
    public void testDelete() {
        store.put("key1", "value1");
        String deletedValue = store.delete("key1");

        assertEquals("value1", deletedValue);
        assertFalse(store.containsKey("key1"));
        assertEquals(0, store.size());
    }

    @Test
    @DisplayName("Should return null when deleting non-existent key")
    public void testDeleteNonExistentKey() {
        String deletedValue = store.delete("nonexistent");
        assertNull(deletedValue);
    }

    @Test
    @DisplayName("Should check key existence")
    public void testContainsKey() {
        assertFalse(store.containsKey("key1"));

        store.put("key1", "value1");
        assertTrue(store.containsKey("key1"));

        store.delete("key1");
        assertFalse(store.containsKey("key1"));
    }

    @Test
    @DisplayName("Should return correct size")
    public void testSize() {
        assertEquals(0, store.size());

        store.put("key1", "value1");
        assertEquals(1, store.size());

        store.put("key2", "value2");
        assertEquals(2, store.size());

        store.delete("key1");
        assertEquals(1, store.size());
    }

    @Test
    @DisplayName("Should clear all entries")
    public void testClear() {
        store.put("key1", "value1");
        store.put("key2", "value2");
        store.put("key3", "value3");

        assertEquals(3, store.size());

        store.clear();

        assertEquals(0, store.size());
        assertTrue(store.isEmpty());
        assertFalse(store.containsKey("key1"));
    }

    @Test
    @DisplayName("Should return all keys")
    public void testKeys() {
        store.put("key1", "value1");
        store.put("key2", "value2");
        store.put("key3", "value3");

        Set<String> keys = store.keys();

        assertEquals(3, keys.size());
        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
        assertTrue(keys.contains("key3"));
    }

    @Test
    @DisplayName("Should return all key-value pairs")
    public void testGetAll() {
        store.put("key1", "value1");
        store.put("key2", "value2");

        Map<String, String> all = store.getAll();

        assertEquals(2, all.size());
        assertEquals("value1", all.get("key1"));
        assertEquals("value2", all.get("key2"));
    }

    @Test
    @DisplayName("Should get value or default")
    public void testGetOrDefault() {
        store.put("key1", "value1");

        assertEquals("value1", store.getOrDefault("key1", "default"));
        assertEquals("default", store.getOrDefault("nonexistent", "default"));
    }

    @Test
    @DisplayName("Should put if absent")
    public void testPutIfAbsent() {
        String previous = store.putIfAbsent("key1", "value1");
        assertNull(previous);
        assertEquals("value1", store.get("key1").get());

        previous = store.putIfAbsent("key1", "value2");
        assertEquals("value1", previous);
        assertEquals("value1", store.get("key1").get());
    }

    @Test
    @DisplayName("Should throw exception for null key on put")
    public void testPutNullKey() {
        assertThrows(IllegalArgumentException.class, () -> store.put(null, "value"));
    }

    @Test
    @DisplayName("Should throw exception for null value on put")
    public void testPutNullValue() {
        assertThrows(IllegalArgumentException.class, () -> store.put("key", null));
    }

    @Test
    @DisplayName("Should throw exception for null key on get")
    public void testGetNullKey() {
        assertThrows(IllegalArgumentException.class, () -> store.get(null));
    }

    @Test
    @DisplayName("Should throw exception for null key on update")
    public void testUpdateNullKey() {
        assertThrows(IllegalArgumentException.class, () -> store.update(null, "value"));
    }

    @Test
    @DisplayName("Should throw exception for null value on update")
    public void testUpdateNullValue() {
        assertThrows(IllegalArgumentException.class, () -> store.update("key", null));
    }

    @Test
    @DisplayName("Should throw exception for null key on delete")
    public void testDeleteNullKey() {
        assertThrows(IllegalArgumentException.class, () -> store.delete(null));
    }

    @Test
    @DisplayName("Should support different data types")
    public void testDifferentDataTypes() {
        InMemoryKeyValueStore<String, Integer> intStore = new InMemoryKeyValueStore<>();
        intStore.put("count", 42);
        assertEquals(42, intStore.get("count").get());

        InMemoryKeyValueStore<Integer, String> reverseStore = new InMemoryKeyValueStore<>();
        reverseStore.put(1, "one");
        assertEquals("one", reverseStore.get(1).get());
    }

    @Test
    @DisplayName("Should handle multiple operations in sequence")
    public void testMultipleOperations() {
        store.put("user1", "Alice");
        store.put("user2", "Bob");
        store.put("user3", "Charlie");

        assertEquals(3, store.size());

        store.update("user2", "Bobby");
        assertEquals("Bobby", store.get("user2").get());

        store.delete("user3");
        assertEquals(2, store.size());

        store.putIfAbsent("user3", "Chuck");
        assertEquals("Chuck", store.get("user3").get());

        store.clear();
        assertTrue(store.isEmpty());
    }
}
