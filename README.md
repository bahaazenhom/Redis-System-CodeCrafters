[![progress-banner](https://backend.codecrafters.io/progress/redis/9f6f4c11-1d66-48c8-8a2a-8ff286b6fb5e)](https://app.codecrafters.io/users/codecrafters-bot?r=2qF)

# Redis Java Implementation

A comprehensive Redis clone implementation in Java, built as part of the [CodeCrafters "Build Your Own Redis" Challenge](https://codecrafters.io/challenges/redis).

## Overview

This project implements a fully-functional Redis server in Java with support for multiple data structures, persistence, replication, pub/sub messaging, geospatial queries, and transactions. The implementation uses clean architecture principles with separation of concerns across command handlers, storage repositories, and protocol layers.

## Features

### Core Data Structures
- **Strings**: GET, SET, INCR, and related operations
- **Lists**: LPUSH, RPUSH, LPOP, LRANGE, BLPOP with blocking support
- **Sorted Sets**: ZADD, ZRANGE, ZSCORE, ZRANK, ZCARD, ZREM
- **Streams**: XADD, XRANGE, XREAD with blocking capabilities
- **Geospatial**: GEOADD, GEODIST, GEOPOS, GEOSEARCH with Haversine distance calculations

### Advanced Features
- **Replication**: Master-slave replication with PSYNC partial resynchronization
- **Persistence**: RDB file format for data serialization and recovery
- **Pub/Sub Messaging**: PUBLISH, SUBSCRIBE, UNSUBSCRIBE for event-driven communication
- **Transactions**: MULTI, EXEC, DISCARD for atomic command execution
- **Geohashing**: 52-bit geohash encoding using bit interleaving for efficient location storage

### Protocol & Concurrency
- **RESP (Redis Serialization Protocol)**: Full support for parsing and serializing RESP arrays, bulk strings, integers, and error responses
- **Concurrent Client Handling**: Multi-threaded client connection management with UUID-based client tracking using dedicated threads per client
- **Thread-Safe Synchronization**: ConcurrentHashMap and synchronized collections for safe concurrent access across multiple client threads
- **Blocking Operations**: Non-blocking event-driven wait registry system for BLPOP and XREAD with configurable timeouts
- **Lock-Free Architecture**: Leverages Java's concurrent utilities to minimize contention and maximize throughput

## Architecture

### Project Structure
```
src/main/java/
├── Main.java                 # Entry point and server initialization
├── command/                  # Command execution framework
│   ├── CommandFactory.java
│   ├── CommandStrategy.java
│   └── handlers/            # Command-specific handlers organized by domain
│       ├── connection/
│       ├── geospatial/
│       ├── list/
│       ├── pubsub/
│       ├── replication/
│       ├── sortedset/
│       ├── stream/
│       ├── string/
│       └── transaction/
├── protocol/                # RESP protocol implementation
│   ├── RESPParser.java
│   └── RESPSerializer.java
├── storage/                 # Data persistence layer
│   ├── DataStore.java      # Interface for all repository operations
│   ├── InMemoryDataStore.java
│   ├── repository/         # Domain-specific repositories
│   ├── impl/              # Repository implementations
│   └── concurrency/       # Thread-safe utilities
├── server/                 # Network and connection handling
│   ├── core/
│   └── handler/
├── rdb/                    # RDB file format handling
│   ├── RDBManager.java
│   ├── RDBReader.java
│   ├── RDBWriter.java
│   └── util/
├── replication/            # Master-slave replication logic
│   ├── MasterNode.java
│   ├── SlaveNode.java
│   ├── ReplicationManager.java
│   └── sync/
├── pub/sub/               # Pub/Sub event system
│   └── ChannelManager.java
├── domain/                # Data models
│   ├── DataType.java
│   ├── RedisValue.java
│   └── values/            # Specific data type implementations
└── util/                  # Utility functions
    ├── GeospatialEncoding.java
    ├── GeospatialDecoding.java
    └── AppLogger.java
```

## Key Implementation Details

### Geospatial Query System
- **Encoding**: Normalizes latitude/longitude to 26-bit precision and interleaves bits for 52-bit geohash
- **Decoding**: Reconstructs coordinates from geohash through bit de-interleaving and normalization
- **Distance Calculation**: Implements Haversine formula for accurate great-circle distance measurements
- **Registry System**: Maintains separate tracking of geospatial keys for efficient GEOSEARCH queries

### Replication System
- **Master**: Handles write commands and maintains replication offset
- **Slave**: Connects to master and maintains synchronization state
- **PSYNC**: Partial resynchronization protocol for efficient recovery
- **WAITs**: ACK-based synchronization for write durability guarantees

### Persistence (RDB)
- **Format**: Redis Database (RDB) binary format support
- **Encoding**: Variable-length encoding for strings and metadata
- **Recovery**: Automatic loading of persisted data on startup

### Transaction Support
- **MULTI/EXEC**: Commands queued during transaction context and executed atomically
- **DISCARD**: Rollback of queued commands

## Getting Started

### Build and Run
```bash
# Compile the project
mvn clean compile

# Package
mvn package

# Run
./your_program.sh
```

### Testing
The implementation has been tested with the CodeCrafters test suite across all major Redis features.

## Technologies Used
- **Java 17**: Modern Java with records, text blocks, and switch expressions
- **Maven**: Dependency management and build automation
- **Concurrent Collections**: ConcurrentHashMap, ConcurrentHashSet for thread-safe data structures
- **Multi-threading**: Dedicated threads per client connection and asynchronous replication handlers
- **Synchronization Primitives**: ReentrantLock, CountDownLatch, and custom wait registries for coordination between threads
- **RESP Protocol**: Binary-safe protocol for Redis compatibility

## Performance Considerations
- **O(1) Lookups**: Hash-based storage for instant key access
- **Efficient Geospatial Queries**: Registry-based iteration over geospatial keys only
- **Non-blocking Blocking Operations**: Event-driven wait registry system
- **Thread-safe Operations**: ConcurrentHashMap and synchronized collections throughout
- **Multi-threaded Scalability**: Per-client dedicated threads for concurrent request processing without thread pool overhead
- **Reduced Lock Contention**: Fine-grained locking strategy with concurrent collections minimizing global synchronization overhead
- **Wait Registry Pattern**: Efficient blocking list and stream operations using event-driven callbacks instead of busy-waiting

## Notes
The entry point for the Redis implementation is in `src/main/java/Main.java`.

