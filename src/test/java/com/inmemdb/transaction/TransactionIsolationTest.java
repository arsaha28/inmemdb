package com.inmemdb.transaction;

import com.inmemdb.storage.InMemoryKeyValueStore;
import com.inmemdb.storage.KeyValueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for transaction isolation and decoupling from storage
 * Verifies that transactions use deep copies and don't share object references
 */
@DisplayName("Transaction Isolation Tests")
public class TransactionIsolationTest {
    private KeyValueStore<String, Object> store;
    private TransactionManager txManager;

    /**
     * Mutable test class to verify deep copying
     */
    static class MutableData implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private int value;
        private List<String> items;

        public MutableData(String name, int value) {
            this.name = name;
            this.value = value;
            this.items = new ArrayList<>();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public List<String> getItems() {
            return items;
        }

        public void addItem(String item) {
            this.items.add(item);
        }
    }

    @BeforeEach
    public void setUp() {
        store = new InMemoryKeyValueStore<>();
        txManager = new TransactionManager(store);
    }

    @Test
    @DisplayName("Should isolate transaction from external object modifications")
    public void testExternalModificationIsolation() throws TransactionException {
        MutableData data = new MutableData("original", 100);
        data.addItem("item1");

        Transaction tx = txManager.beginTransaction();
        tx.put("data", data);

        // Modify the original object after putting it in transaction
        data.setName("modified");
        data.setValue(200);
        data.addItem("item2");

        // Transaction should still have the original values (deep copy)
        MutableData txData = (MutableData) tx.get("data").orElseThrow();
        assertEquals("original", txData.getName());
        assertEquals(100, txData.getValue());
        assertEquals(1, txData.getItems().size());
        assertEquals("item1", txData.getItems().get(0));

        tx.commit();
    }

    @Test
    @DisplayName("Should isolate returned objects from transaction modifications")
    public void testReturnedObjectIsolation() throws TransactionException {
        MutableData original = new MutableData("data", 50);

        Transaction tx = txManager.beginTransaction();
        tx.put("data", original);

        // Get the object from transaction
        MutableData retrieved = (MutableData) tx.get("data").orElseThrow();

        // Modify the retrieved object
        retrieved.setName("modified_retrieved");
        retrieved.setValue(999);

        // Get again from transaction - should not reflect the modification
        MutableData retrievedAgain = (MutableData) tx.get("data").orElseThrow();
        assertEquals("data", retrievedAgain.getName());
        assertEquals(50, retrievedAgain.getValue());

        tx.commit();
    }

    @Test
    @DisplayName("Should isolate store from uncommitted transaction changes")
    public void testStoreIsolationBeforeCommit() {
        MutableData data = new MutableData("txData", 300);

        Transaction tx = txManager.beginTransaction();
        tx.put("key1", data);

        // Store should not have the data before commit
        assertFalse(store.containsKey("key1"));

        // Even if we modify the original object, store shouldn't be affected
        data.setName("changed");
        assertFalse(store.containsKey("key1"));
    }

    @Test
    @DisplayName("Should isolate committed data from original object modifications")
    public void testCommittedDataIsolation() throws TransactionException {
        MutableData data = new MutableData("committed", 400);
        data.addItem("A");

        Transaction tx = txManager.beginTransaction();
        tx.put("key1", data);
        tx.commit();

        // After commit, modify the original object
        data.setName("modified_after_commit");
        data.setValue(500);
        data.addItem("B");

        // Store should have the committed values, not the modified ones
        MutableData storedData = (MutableData) store.get("key1").orElseThrow();
        assertEquals("committed", storedData.getName());
        assertEquals(400, storedData.getValue());
        assertEquals(1, storedData.getItems().size());
        assertEquals("A", storedData.getItems().get(0));
    }

    @Test
    @DisplayName("Should isolate transaction from store modifications")
    public void testTransactionIsolationFromStoreChanges() throws TransactionException {
        // Put data in store
        MutableData storeData = new MutableData("inStore", 100);
        store.put("shared", storeData);

        // Start transaction and read the data
        Transaction tx = txManager.beginTransaction();
        MutableData txData = (MutableData) tx.get("shared").orElseThrow();

        assertEquals("inStore", txData.getName());
        assertEquals(100, txData.getValue());

        // Modify store data directly
        MutableData originalRef = (MutableData) store.get("shared").orElseThrow();
        originalRef.setName("modifiedInStore");
        originalRef.setValue(200);

        // Transaction should still see original values (it read a copy)
        MutableData txDataAgain = (MutableData) tx.get("shared").orElseThrow();
        assertEquals("inStore", txDataAgain.getName());
        assertEquals(100, txDataAgain.getValue());

        tx.rollback();
    }

    @Test
    @DisplayName("Should handle nested mutable objects correctly")
    public void testNestedMutableObjectsIsolation() throws TransactionException {
        MutableData data = new MutableData("parent", 10);
        data.addItem("child1");
        data.addItem("child2");

        Transaction tx = txManager.beginTransaction();
        tx.put("data", data);

        // Modify nested collection in original
        data.getItems().clear();
        data.addItem("different");

        // Transaction should have original nested data
        MutableData txData = (MutableData) tx.get("data").orElseThrow();
        assertEquals(2, txData.getItems().size());
        assertEquals("child1", txData.getItems().get(0));
        assertEquals("child2", txData.getItems().get(1));

        tx.commit();
    }

    @Test
    @DisplayName("Should isolate multiple concurrent transactions")
    public void testMultipleTransactionIsolation() throws TransactionException {
        MutableData initialData = new MutableData("initial", 0);
        store.put("counter", initialData);

        // Start two transactions
        Transaction tx1 = txManager.beginTransaction();
        Transaction tx2 = txManager.beginTransaction();

        // Both read the same key
        MutableData tx1Data = (MutableData) tx1.get("counter").orElseThrow();
        MutableData tx2Data = (MutableData) tx2.get("counter").orElseThrow();

        // Modify in tx1
        tx1Data.setValue(100);
        tx1.put("counter", tx1Data);

        // Modify in tx2 (should be independent)
        tx2Data.setValue(200);
        tx2.put("counter", tx2Data);

        // Each transaction should see its own modifications
        assertEquals(100, ((MutableData) tx1.get("counter").orElseThrow()).getValue());
        assertEquals(200, ((MutableData) tx2.get("counter").orElseThrow()).getValue());

        // Commit tx1
        tx1.commit();
        assertEquals(100, ((MutableData) store.get("counter").orElseThrow()).getValue());

        // Commit tx2 (overwrites tx1)
        tx2.commit();
        assertEquals(200, ((MutableData) store.get("counter").orElseThrow()).getValue());
    }

    @Test
    @DisplayName("Should throw exception for non-serializable objects")
    public void testNonSerializableObjectsRejected() {
        // Create a non-serializable object
        Object nonSerializable = new Object() {
            private String value = "test";
        };

        Transaction tx = txManager.beginTransaction();

        // Should throw IllegalArgumentException when trying to store non-serializable
        assertThrows(IllegalArgumentException.class, () -> {
            tx.put("key", nonSerializable);
        });
    }

    @Test
    @DisplayName("Should handle immutable objects efficiently")
    public void testImmutableObjectsNotCopied() throws TransactionException {
        // Immutable types should not be copied (optimization)
        String strValue = "immutable string";
        Integer intValue = 42;

        Transaction tx = txManager.beginTransaction();
        tx.put("str", strValue);
        tx.put("int", intValue);

        // Should work fine with immutable types
        assertEquals(strValue, tx.get("str").orElseThrow());
        assertEquals(intValue, tx.get("int").orElseThrow());

        tx.commit();

        assertEquals(strValue, store.get("str").orElseThrow());
        assertEquals(intValue, store.get("int").orElseThrow());
    }
}
