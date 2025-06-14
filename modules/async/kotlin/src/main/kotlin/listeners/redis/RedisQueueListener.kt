package listeners.redis

import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.api.sync.RedisCommands
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import kotlin.concurrent.thread

/**
 * RedisQueueListener is a component that listens to messages published on a Redis queue
 * and processes them asynchronously. It deserializes incoming messages, calculates latency,
 * and logs relevant information.
 *
 * @property commands Connection interface for executing Redis commands.
 * @property objectMapper ObjectMapper instance for JSON deserialization.
 */
@Component
class RedisQueueListener(
    private val commands: RedisCommands<String, String>,
    private val redisQueueHandler: RedisQueueHandler)
{
    val logger = KotlinLogging.logger {}
    private val queueKey = "benchmarkQueue"

    /**
     * Starts listening to a Redis queue for incoming messages and processes them asynchronously.
     *
     * This method runs a background thread that continuously polls a Redis queue using the BRPOP
     * command with a timeout of 1 second. If a message is received, it is handled in a separate
     * virtual thread for processing, ensuring efficient use of system resources.
     * The `processMessage` function is used to handle the deserialization and processing of the
     * received message.
     *
     * Potential message types include:
     * - CREATE_MESSAGE: Handles creation of new data.
     * - READ_MESSAGE: Handles reading data and counting lines.
     * - UPDATE_MESSAGE: Handles updating and modifying data.
     * - DELETE_MESSAGE: Handles deletion of data and related state cleanup.
     *
     * Errors during message deserialization or processing are logged.
     *
     * The method leverages Java Loom's virtual threads to scale processing efficiently for high-throughput scenarios.
     *
     * Note: This method is marked with `@PostConstruct`, meaning it is triggered automatically
     * after the construction of the related bean and its dependencies.
     */
    @PostConstruct
    fun startListening()
    {
        thread(start = true)
        {
            while (true)
            {
                val message = commands.brpop(1, queueKey)
                if (message != null)
                {
                    redisQueueHandler.processMessage(message.value)
                }
            }
        }
    }
}
