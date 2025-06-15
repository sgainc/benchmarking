## Overview

This document defines the rules, requirements, and guidelines for a hackathon challenge in which participants choose any programming language or framework to build an asynchronous cloud-based load-testing service. The goal is to design, implement, deploy, and tune a service that generates, processes, and monitors messages via Redis and S3, running continuously for 24 hours on AWS ECS Fargate. Teams will repeat deployments and optimizations until reaching saturation, then report resource usage and throughput.

---

## Challenge Description

* **Objective**: Build an asynchronous service that load-tests your chosen language/framework by generating and processing messages according to specified distributions, leveraging Redis as a message queue and S3 for file storage operations.
* **Duration**: The service must run continuously for 24 hours in AWS ECS Fargate.
* **Health Check**: Expose a single HTTP(S) endpoint (e.g., `/health`) that returns a simple “OK” (HTTP 200) to confirm the service is alive and responsive.

---

## Technical Requirements

1. **Message Queue**

   * Use a Redis list as the message queue.
   * Producers push messages (e.g., LPUSH) into the Redis list; consumers pop messages (e.g., BRPOP or other non-blocking patterns depending on language capabilities).

2. **Message Payloads & File Sizes**

   * Each message corresponds to a logical operation (READ, UPDATE, CREATE, DELETE).
   * For CREATE operations, generate a text file containing between 1,000 and 10,000 random UUIDs. (Number of UUIDs per file should be uniformly random in that range, configurable if desired.)

3. **Task Distribution Logic**

   * **Preconditions** (before entering steady-state):

     * Inspect the S3 bucket (or a designated prefix) for the count of objects:

       * If fewer than 100 objects exist, enqueue CREATE tasks until reaching ≥100.
       * If more than 1,000 objects exist, enqueue DELETE tasks to reduce count to ≤1,000.
     * This ensures a baseline inventory of files before steady-state.
   * **Steady-State Distribution** (once preconditions met continuously):

     * 50% READ operations
     * 25% UPDATE operations
     * 10% CREATE operations
     * 15% DELETE operations
   * Implementation approach:

     * A scheduler component (asynchronous loop or scheduled job) that, at a configurable rate (e.g., every second or batch interval), enqueues messages according to the above percentages.
     * Use a weighted-random or token-bucket approach to approximate percentages over time.
     * Each message includes metadata indicating operation type and any necessary parameters (e.g., S3 key, byte offsets for UPDATE, file size, etc.).
   * **Operation Details**:

     * **READ**: Download the entire file from S3 (GET).
     * **UPDATE**: Download file (GET), select two random hex characters in its content, replace them with two other random hex characters, then re-upload (PUT). Ensure that the file remains valid text.
     * **CREATE**: Generate between 1,000 and 10,000 random UUIDs; write them line-by-line into a temporary buffer or file, then upload to S3 with a unique key.
     * **DELETE**: Remove the specified file from S3 (DELETE).
   * **Key Selection**:

     * For READ/UPDATE/DELETE, pick an existing object at random (or round-robin). For CREATE, generate a new unique key (e.g., with UUID or timestamp suffix).
     * The scheduler must maintain or fetch the list of existing keys (e.g., periodically list S3 prefix) to ensure operations target valid objects.

4. **Logging and Metrics**

   * **Queue Length**: Every 10 seconds, log the current length of the Redis list (e.g., using LLEN).
   * **Messages Created**: Maintain a counter of messages enqueued; log cumulative count periodically (e.g., every 10 seconds).
   * **Messages Processed Successfully**: Count successful completions of each operation; log cumulative or interval count every 10 seconds.
   * **Per-Second Rate**: Every 10 seconds, calculate and log the average messages processed per second over the last interval.
   * **Runtime Errors**: Log all exceptions or errors during Redis operations, S3 interactions, or internal logic. Include timestamp, operation type, and error details.
   * **Additional Metrics (Optional but Recommended)**:

     * CPU and memory utilization within the container (via metrics endpoint or sidecar).
     * Latency distributions for each operation type.
     * Redis latency (e.g., response times for queue operations).
     * S3 operation latencies.
   * **Instrumentation**: Use language/framework–appropriate logging libraries. Format logs for easy aggregation (e.g., JSON lines). Optionally, push metrics to CloudWatch or another monitoring system.

5. **Asynchronous Design & Separation of Concerns**

   * **Architecture Layers**:

     * **Configuration**: Load environment variables or config files for Redis endpoint, S3 bucket, file size bounds, distribution percentages, logging settings, ECS-related settings.
     * **Controllers / API**: Implement the health-check endpoint in a minimal web server (e.g., HTTP handler returning 200). No other external endpoints required.
     * **Services**:

       * **RedisService**: Abstract Redis interactions (enqueue, dequeue, metrics).
       * **S3Service**: Abstract S3 operations (list, get, put, delete).
       * **SchedulerService**: Periodically makes decisions to enqueue messages based on preconditions and steady-state logic.
       * **WorkerService**: Consumes messages asynchronously, dispatches to operation-specific handlers.
     * **DTO / Message Models**: Define a message schema (e.g., JSON) with fields: operation type, target key or parameters, timestamp, retries count.
     * **Event Listeners / Handlers**: For each operation type (READ, UPDATE, CREATE, DELETE), implement handlers that perform the S3 logic.
     * **Tasks / Job Runners**: If the framework supports job/task abstractions (e.g., async tasks, thread pools), encapsulate work accordingly.
     * **Logging / Metrics Module**: Centralize logging calls and metric collection.
     * **Error Handling**: Wrap operations with try/catch (or equivalent), increment failure counters, optionally re-enqueue or drop after retries.
   * **Concurrency Model**: Use the language’s asynchronous primitives (e.g., async/await, coroutines, event loops, threads) to allow concurrent processing of messages. Ensure proper connection pooling for Redis and S3 clients.

6. **Deployment: AWS ECS Fargate**

   * **Containerization**: Provide a Dockerfile that packages the service.
   * **ECS Task Definition**:

     * Configure CPU and memory reservations; limit to 1 vCPU.
     * Set environment variables for Redis endpoint, S3 bucket name, file size bounds, distribution percentages, logging/monitoring settings.
   * **Redis**: Provision a Redis instance (e.g., AWS ElastiCache) externally; connection info provided via environment variables. If bottleneck occurs, teams may scale up the Redis instance.
   * **S3**: Use a designated S3 bucket with appropriate permissions (IAM roles).
   * **Logging & Monitoring**: Configure AWS CloudWatch Logs for container logs. Optionally, push custom metrics to CloudWatch.
   * **Health Check**: Configure ECS to use the `/health` endpoint for container health.
   * **Execution Duration**: The task runs continuously for 24 hours. Participants may schedule the task to stop after 24 hours or manually terminate.
   * **Autoscaling / Parallelism**: We are only measuring the performance of a single vCPU deployment. 

7. **Performance Tuning & Reporting**

   * **Deploy, Measure, Tune**:

     * Start with moderate concurrency (e.g., number of worker threads or async tasks).
     * Monitor CPU utilization, memory, Redis and S3 latencies, message throughput.
     * Incrementally increase concurrency or resource allocations until saturation (e.g., CPU near limit, Redis latency increases, or S3 throttling).
     * Document changes: number of threads/goroutines/coroutines, CPU/memory settings in Fargate, Redis instance size, etc.
   * **Reporting Format**: Provide a summary report (e.g., Markdown or PDF) that includes:

     * Configuration parameters (language, framework version, container CPU/memory, Redis instance type, file size bounds, distribution percentages).
     * Concurrency settings (number of workers/tasks).
     * Observed metrics at each tuning stage: CPU utilization (%), memory usage (MB), queue length dynamics, per-second throughput, latencies, error rates.
     * Point of saturation: describe which resource became the bottleneck (CPU, Redis, S3, network).
     * Any mitigation steps (e.g., scaling Redis, adjusting batch sizes).
     * Final achieved throughput (messages/second) under steady-state distribution.
   * **Presentation**: Graphs or tables illustrating throughput vs. concurrency/CPU, latency distributions, queue length over time. (Optional: include sample scripts or dashboards.)

8. **Submission Guidelines**

   * **Code & Configuration**:

     * Upload source code to this repository with clear README explaining how to build, configure, and deploy.
     * Include Dockerfile and any ECS deployment artifacts (e.g., CloudFormation templates or Terraform snippets if used).
     * Document environment variables and defaults.
   * **Instructions**: Step-by-step guide: how to set up Redis and S3 bucket, deploy to ECS Fargate, run for 24 hours, collect logs/metrics.
   * **Report**: Submit a report as described in the Performance Tuning & Reporting section.
   * **Logs & Metrics Archive**: Optionally, include sample logs or metric exports that demonstrate the service behavior (e.g., JSON log snippets, CloudWatch metric screenshots or exported CSV).
   * **Evaluation Criteria**:

     * **Correctness**: Adherence to requirements (Redis queue usage, S3 operations per distribution, logging intervals, health check).
     * **Asynchronous Design & Code Quality**: Proper separation of concerns, clear comments on classes/methods, robust error handling, maintainability.
     * **Performance**: Achieved throughput, efficient resource use, evidence of tuning and reasoning about bottlenecks.
     * **Scalability Considerations**: How the design could scale (e.g., adjusting container count, Redis scaling).
     * **Documentation**: Clarity of README and report, ease of deployment following instructions.
     * **Creativity (Optional)**: Additional useful features, e.g., advanced metrics dashboards, dynamic scaling logic, retry/backoff strategies, configurable UI or monitoring endpoints.

9. **Style Guidelines**

   * **Code Comments**: Every class/module and public method/function must include clear comments describing purpose, inputs, outputs, side effects, and error conditions.
   * **Separation of Concerns**: Organize code into layers or packages/modules:

     * Configuration loading
     * API/controllers (health endpoint)
     * Services (RedisService, S3Service, SchedulerService, WorkerService)
     * DTOs/message schemas
     * Utilities (logging, metrics)
     * Event listeners or handlers for operation types
     * Error-handling and retry logic in dedicated modules
   * **Configuration Management**: Use environment variables or configuration files; avoid hard-coded values.
   * **Dependency Injection (if applicable)**: Where language/framework supports DI, inject services for easier testing and modularity.
   * **Logging**: Use structured logging (JSON or similar) to allow easy parsing. Include timestamps, log levels, operation type, metrics values.
   * **Testing (Optional but Encouraged)**: Provide unit tests or integration tests for core components (e.g., message creation logic, file manipulation for UPDATE). Mock Redis/S3 where possible.
   * **Resource Cleanup**: Ensure that on shutdown or after 24 hours, resources (e.g., open connections) are cleaned up gracefully.
   * **Retries and Backoff**: For transient failures (e.g., network errors to Redis or S3), implement retry logic with exponential backoff and a max retry limit; log failures that exceed retries.

10. **Process & Workflow**

    1. **Initial Setup**

       * Provision Redis instance and S3 bucket, ensuring permissions.
       * Clone starter repository or initialize new project with chosen language/framework.
       * Implement RedisService and S3Service abstractions; write simple smoke tests.
       * Add health endpoint. Containerize and verify locally (or in a test environment).
    2. **Queueing & Scheduler**

       * Implement scheduler for preconditions: periodically check object count in S3, enqueue CREATE/DELETE until count in \[100, 1,000].
       * After preconditions, switch to steady-state distribution logic.
       * Enqueue messages continuously at a controlled rate (configurable).
    3. **Workers & Handlers**

       * Implement asynchronous workers to consume messages and perform S3 operations.
       * Instrument logging for start, success, failure of each operation.
    4. **Logging & Metrics**

       * Add periodic tasks (every 10 seconds) to log queue length, counts, rates, errors.
       * Optionally integrate with CloudWatch metrics or other monitoring.
    5. **Deployment & Baseline Run**

       * Deploy to ECS Fargate with minimal resources. Run for a short test (e.g., 1 hour) to verify correctness.
       * Collect logs and metrics, confirm health endpoint works.
    6. **Tuning & Scaling**

       * Gradually increase concurrency (number of workers, threads, tasks). Observe CPU, memory, Redis latency, S3 throttling.
       * Scale container CPU/memory or number of container instances if parallel deployment is used.
       * If Redis shows latency, consider increasing its instance size or enabling clustering.
       * Document each change and its impact on throughput and resource usage.
    7. **24-Hour Run & Reporting**

       * Execute full 24-hour run, capture logs/metrics continuously.
       * After completion, generate the performance report with graphs/tables.
       * Package code, instructions, and report for submission.

11. **Evaluation & Scoring**

    * **Functional Compliance** (30%): Correct use of Redis queue, S3 operations per distribution, logging intervals, health endpoint, 24-hour run.
    * **Code Quality & Architecture** (20%): Clear separation of concerns, comments, configuration management, error handling, test coverage.
    * **Performance & Tuning** (30%): Demonstrated iterative tuning, documented saturation point, resource utilization graphs, achieved throughput.
    * **Documentation & Usability** (10%): Clarity of README, ease of deployment, explanation of metrics and logs.
    * **Innovation & Extras** (10%): Bonus features such as dynamic scaling, advanced monitoring dashboards, adaptive scheduling, or additional insights.

---

## Example Configuration (Illustrative)

```yaml
# Environment variables or config file
REDIS_ENDPOINT=redis://my-redis-endpoint:6379
S3_BUCKET=my-loadtest-bucket
PRECONDITION_MIN_OBJECTS=100
PRECONDITION_MAX_OBJECTS=1000
STEADY_STATE_DISTRIBUTION:
  READ: 50
  UPDATE: 25
  CREATE: 10
  DELETE: 15
SCHEDULER_INTERVAL_SECONDS=1
LOG_INTERVAL_SECONDS=10
WORKER_CONCURRENCY=10            # initial value; tune upward
AWS_REGION=us-east-1
HEALTH_ENDPOINT_PATH=/health
```

---

## Tips & Recommendations

* **Asynchronous Libraries**: Choose high-performance async libraries (e.g., Node.js with async/await, Python asyncio or languages with native concurrency like Go, Rust async frameworks, Java with reactive libraries, Kotlin coroutines, etc.).
* **Connection Pooling**: Ensure Redis and S3 clients are pooled or reused; avoid reconnecting on every operation.
* **S3 Pagination**: For listing objects (to count or pick random keys), handle pagination efficiently. Cache object lists if appropriate, but refresh periodically to reflect CREATE/DELETE.
* **Randomness**: Use cryptographically secure RNG only if needed; otherwise, standard RNG is fine for randomness in file sizes and UUID generation.
* **Error Scenarios**: Plan for S3 eventual consistency or transient errors; consider that DELETE may not immediately reflect in subsequent LIST operations—handle gently.
* **Resource Limits**: Monitor container limits; avoid out-of-memory or file descriptor exhaustion.
* **Logging Overhead**: Structure logs to avoid overwhelming I/O; consider asynchronous or buffered logging if high throughput.
* **Monitoring Tools**: Optionally integrate AWS X-Ray or third-party APM for deeper insights.
* **Automated Testing**: Write unit tests for message creation and processing logic, mocking Redis and S3. This helps ensure correctness before large-scale runs.
* **Clean Shutdown**: Handle SIGTERM to gracefully stop consuming new messages and flush logs.
* **Cost Awareness**: Running for 24 hours with high throughput may incur AWS costs (S3 requests, data transfer, Redis instance hours). Monitor costs and consider simulated runs or smaller scale if budget-constrained.

---

## FAQ

1. **Can we use multiple ECS tasks in parallel?**
   No, we are only testing a single instance limited to one vCPU.

2. **What counts as “file size random between X and X”?**
   Use configuration (environment variables) to specify `MIN_FILE_SIZE_BYTES` and `MAX_FILE_SIZE_BYTES`. The service should randomly choose a size in that range for new or updated files.

3. **How to implement weighted distribution?**
   Over each scheduling interval, generate a random number and map to operations by cumulative weight (e.g., 0–49 → READ, 50–74 → UPDATE, etc.). Alternatively, use a scheduler that tracks counts to approach percentages over time.

4. **How to observe queue length?**
   Use Redis LLEN command periodically. Log the result.

5. **What health endpoint is required?**
   A minimal HTTP GET endpoint (e.g., `/health`) returning HTTP 200 and a simple JSON or plaintext body like “OK”. ECS uses this to verify container health.

6. **How to handle Redis as a bottleneck?**
   If Redis latency or errors increase significantly under load, scale up the Redis instance (e.g., larger ElastiCache node) or use clustering. Document the decision and its impact.

7. **What if S3 throttles requests?**
   S3 has high request capacity, but under extreme loads you may see occasional throttling. Implement retry with backoff. Consider spreading requests across prefixes or use S3 Transfer Acceleration if needed.

---

## Summary

Participants must design an asynchronous, modular service using any language/framework to generate and process messages via Redis and S3 according to specified precondition and steady-state distributions. The service runs for 24 hours in ECS Fargate, logs metrics every 10 seconds, exposes a health endpoint, and is tuned iteratively to reach saturation. Clear code structure, proper comments, robust error handling, and thorough reporting of performance metrics are essential. Submissions will be evaluated on correctness, architecture, performance tuning, documentation, and any innovative enhancements. Good luck, and may your service achieve high throughput and stability!
