package com.inmemdb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InMemDB
 */
public class InMemDBTest {
    private InMemDB db;

    @BeforeEach
    public void setUp() {
        db = new InMemDB();
    }

    @Test
    public void testDatabaseInitialization() {
        assertNotNull(db, "InMemDB should be initialized");
    }

    @Test
    public void testDatabaseRun() {
        assertDoesNotThrow(() -> db.run(), "Database run should not throw exceptions");
    }
}
