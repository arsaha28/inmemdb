package com.inmemdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * InMemDB - In-Memory Database
 * Main entry point for the in-memory database
 */
public class InMemDB {
    private static final Logger logger = LoggerFactory.getLogger(InMemDB.class);

    public InMemDB() {
        logger.info("InMemDB initialized");
    }

    public void run() {
        logger.info("InMemDB running...");
        System.out.println("InMemDB is ready!");
    }

    public static void main(String[] args) {
        logger.info("Starting InMemDB application");
        InMemDB db = new InMemDB();
        db.run();
    }
}
