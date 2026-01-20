/**
 * Transaction management for InMemDB with complete isolation from storage layer.
 *
 * <h2>Architecture Overview</h2>
 * <p>
 * The transaction layer is completely decoupled from the storage layer through
 * deep copying of values. This ensures true ACID-like isolation properties.
 * </p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li><strong>Transaction</strong> - Interface defining transaction lifecycle and operations</li>
 *   <li><strong>TransactionImpl</strong> - Implementation using workspace pattern with deep copies</li>
 *   <li><strong>TransactionManager</strong> - Manages transaction lifecycle and coordination</li>
 *   <li><strong>ValueCopier</strong> - Utility for deep copying objects to ensure isolation</li>
 *   <li><strong>TransactionStatus</strong> - Enum tracking transaction state</li>
 *   <li><strong>TransactionException</strong> - Exception for transaction failures</li>
 * </ul>
 *
 * <h2>Isolation Mechanism</h2>
 * <p>
 * Transactions maintain complete isolation through deep copying:
 * </p>
 * <ol>
 *   <li><strong>On PUT</strong>: Values are deep copied before storing in transaction workspace</li>
 *   <li><strong>On GET</strong>: Values are deep copied when returned (both from workspace and store)</li>
 *   <li><strong>On COMMIT</strong>: Workspace copies are committed to store</li>
 * </ol>
 * <p>
 * This ensures:
 * <ul>
 *   <li>External modifications to objects don't affect transaction state</li>
 *   <li>Modifications to retrieved objects don't affect transaction state</li>
 *   <li>Transaction changes are invisible to store until commit</li>
 *   <li>Multiple transactions are completely isolated from each other</li>
 * </ul>
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create database with transaction support
 * InMemDB db = new InMemDB();
 *
 * // Automatic transaction management
 * db.executeTransaction(tx -> {
 *     tx.getStore().put("key1", value1);
 *     tx.getStore().put("key2", value2);
 *     // Automatically commits on success, rollbacks on exception
 * });
 *
 * // Manual transaction control
 * Transaction tx = db.beginTransaction();
 * try {
 *     tx.getStore().put("key", value);
 *     tx.commit();
 * } catch (Exception e) {
 *     tx.rollback();
 * }
 * }</pre>
 *
 * <h2>Serialization Requirements</h2>
 * <p>
 * Objects stored in transactions must be Serializable for deep copying.
 * Immutable types (String, Integer, Long, etc.) are optimized and not copied.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * TransactionManager and individual transactions are thread-safe:
 * <ul>
 *   <li>Concurrent transactions are isolated using workspace pattern</li>
 *   <li>Transaction operations use ConcurrentHashMap for thread safety</li>
 *   <li>Commit operations are synchronized to prevent race conditions</li>
 *   <li>ReadWriteLock available for global lock coordination</li>
 * </ul>
 * </p>
 *
 * @see Transaction
 * @see TransactionManager
 * @see ValueCopier
 */
package com.inmemdb.transaction;
