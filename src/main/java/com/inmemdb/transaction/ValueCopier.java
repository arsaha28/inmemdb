package com.inmemdb.transaction;

import java.io.*;

/**
 * Utility class for deep copying objects to ensure transaction isolation
 * Uses Java serialization for creating independent copies
 */
class ValueCopier {

    /**
     * Create a deep copy of an object using serialization
     * @param value the value to copy
     * @param <T> the type
     * @return a deep copy of the value
     * @throws IllegalArgumentException if value is not serializable
     */
    @SuppressWarnings("unchecked")
    public static <T> T deepCopy(T value) {
        if (value == null) {
            return null;
        }

        // For immutable types, return as-is
        if (isImmutable(value)) {
            return value;
        }

        // For other types, use serialization to create a deep copy
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(value);
            out.flush();
            out.close();

            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream in = new ObjectInputStream(bis);
            return (T) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalArgumentException(
                    "Value must be Serializable for transaction isolation. Type: " +
                    value.getClass().getName(), e);
        }
    }

    /**
     * Check if a value is immutable and can be safely shared without copying
     * @param value the value to check
     * @return true if immutable
     */
    private static boolean isImmutable(Object value) {
        // Common immutable types in Java
        return value instanceof String ||
               value instanceof Integer ||
               value instanceof Long ||
               value instanceof Double ||
               value instanceof Float ||
               value instanceof Short ||
               value instanceof Byte ||
               value instanceof Character ||
               value instanceof Boolean ||
               value.getClass().isEnum();
    }
}
