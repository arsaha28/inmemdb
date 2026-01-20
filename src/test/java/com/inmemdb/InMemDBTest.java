package com.inmemdb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InMemDB
 */
@DisplayName("InMemDB Tests")
public class InMemDBTest {
    private InMemDB db;

    @BeforeEach
    public void setUp() {
        db = new InMemDB();
    }

    @Test
    @DisplayName("Should initialize database")
    public void testDatabaseInitialization() {
        assertNotNull(db, "InMemDB should be initialized");
        assertNotNull(db.getStore(), "Store should be initialized");
        assertTrue(db.getStore().isEmpty(), "Store should be empty on initialization");
    }

    @Test
    @DisplayName("Should run without exceptions")
    public void testDatabaseRun() {
        assertDoesNotThrow(() -> db.run(), "Database run should not throw exceptions");
    }

    @Test
    @DisplayName("Should set and get string value")
    public void testSetAndGetString() {
        db.set("name", "Alice");

        Optional<Object> result = db.get("name");
        assertTrue(result.isPresent());
        assertEquals("Alice", result.get());
    }

    @Test
    @DisplayName("Should set and get integer value")
    public void testSetAndGetInteger() {
        db.set("count", 42);

        Optional<Object> result = db.get("count");
        assertTrue(result.isPresent());
        assertEquals(42, result.get());
    }

    @Test
    @DisplayName("Should delete value")
    public void testDelete() {
        db.set("key1", "value1");
        assertTrue(db.get("key1").isPresent());

        Object deleted = db.delete("key1");
        assertEquals("value1", deleted);
        assertFalse(db.get("key1").isPresent());
    }

    @Test
    @DisplayName("Should handle multiple operations")
    public void testMultipleOperations() {
        db.set("user", "Bob");
        db.set("age", 25);
        db.set("city", "New York");

        assertEquals(3, db.getStore().size());

        db.delete("city");
        assertEquals(2, db.getStore().size());

        assertTrue(db.get("user").isPresent());
        assertTrue(db.get("age").isPresent());
        assertFalse(db.get("city").isPresent());
    }

    @Test
    @DisplayName("Should run demo without exceptions")
    public void testRunDemo() {
        assertDoesNotThrow(() -> db.runDemo(), "Demo should run without exceptions");
    }

    @Test
    @DisplayName("Should access underlying store")
    public void testGetStore() {
        db.set("key", "value");

        assertTrue(db.getStore().containsKey("key"));
        assertEquals(1, db.getStore().size());
    }
}
