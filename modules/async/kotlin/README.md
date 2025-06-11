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

## Folders

| Folder        | Description                                  |
|---------------|----------------------------------------------|
| `application` | Contains the main applcation class           |
| `config`      | Contains spring configuration classes        |
| `controllers` | Contains spring web controllers              |
| `data`        | Contains data access layers                  |
| `dto`         | Contains data storage classes                |
| `listeners`   | Contains event listeners                     |
| `tasks`       | Contains classes that manage scheduled tasks |
 
## Running in Docker/Compose

Note - the tests are currently trying to connect to redis, so you will 
likely need a redis server running during the build process. If you get a
lettuce connetion closed error during building, it should work on a second try. 

```
$ ./gradlew build
$ podman build -t async-bench-kotlin .
$ podman compose build
$ podman compose up
```

## Deploy to ECR and Run on Fargate

```
$ export AWS_ACCESS_KEY_ID=[AWS_ACCESS_KEY_ID]
$ export AWS_SECRET_ACCESS_KEY=[AWS_SECRET_ACCESS_KEY]
$ export AWS_DEFAULT_REGION=us-east-1
$ aws ecr get-login-password | podman login --username AWS --password-stdin 203422683116.dkr.ecr.us-east-1.amazonaws.com

$ podman tag 1ec8184264ac 203422683116.dkr.ecr.us-east-1.amazonaws.com/nw2s-benchmarking:kotlin-0.0.1
$ podman push 203422683116.dkr.ecr.us-east-1.amazonaws.com/nw2s-benchmarking:kotlin-0.0.1

$ podman tag 328464a853ee 203422683116.dkr.ecr.us-east-1.amazonaws.com/nw2s-benchmarking:redis
$ podman push 203422683116.dkr.ecr.us-east-1.amazonaws.com/nw2s-benchmarking:redis
```

