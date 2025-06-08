# Redis Queue Service

A Spring Boot application that implements a Redis-based message queue system using Lettuce async commands. This service provides reactive message processing capabilities with configurable batch operations and monitoring.

## Prerequisites

- JDK 24
- Kotlin 1.9
- Redis Server 7.0+
- Gradle 8.5+

## Technology Stack

- Spring Boot 3.2
- Lettuce Redis Client
- Kotlin Coroutines
- Jackson for JSON processing
- Micrometer for metrics

## Quick Start

### 1. Start Redis Server
Ensure Redis is running locally on the default port (6379). Using Docker:

```
$ bash docker run --name redis -p 6379:6379 -d redis:latest
```

### 2. Run the Application

```
$ bash ./gradlew bootRun --args='--spring.profiles.active=local'
```


## Configuration Properties

| Property | Description | Default |
|----------|-------------|---------|
| `spring.redis.host` | Redis server host | localhost |
| `spring.redis.port` | Redis server port | 6379 |
| `spring.redis.password` | Redis password (optional) | |
| `spring.redis.timeout` | Connection timeout (ms) | 2000 |

