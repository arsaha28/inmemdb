# InMemDB

In-Memory Database - A lightweight in-memory database implementation

## Overview

InMemDB is a Java-based in-memory database project designed for fast data storage and retrieval.

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

### Build

Build the project using Maven:

```bash
mvn clean install
```

### Run

Execute the application:

```bash
mvn exec:java -Dexec.mainClass="com.inmemdb.InMemDB"
```

Or run the compiled JAR:

```bash
java -jar target/inmemdb-0.1.0.jar
```

### Test

Run the test suite:

```bash
mvn test
```

## Project Structure

```
inmemdb/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/inmemdb/    # Java source files
│   │   └── resources/           # Application resources
│   └── test/
│       ├── java/
│       │   └── com/inmemdb/    # Test files
│       └── resources/           # Test resources
├── target/                      # Build output
└── pom.xml                      # Maven configuration
```

## Maven Commands

- `mvn clean` - Clean the project
- `mvn compile` - Compile the source code
- `mvn test` - Run unit tests
- `mvn package` - Package the application as a JAR
- `mvn install` - Install the package into local repository
- `mvn clean install` - Clean and build the project

## License

MIT
