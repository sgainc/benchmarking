package tasks

import com.fasterxml.jackson.databind.ObjectMapper
import dto.BaseMessage
import dto.CreateDataMessage
import dto.DeleteDataMessage
import dto.MessageType
import dto.MessageWrapper
import dto.ReadDataMessage
import dto.UpdateDataMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.api.sync.RedisCommands
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import kotlin.random.Random

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * A component class responsible for generating and queuing test messages in Redis.
 * The messages consist of various message types, such as create, read, update, and delete,
 * which are designed to test and benchmark the message-handling infrastructure.
 *
 * This class ensures that the Redis queue does not fall below a certain threshold of messages
 * by automatically generating and pushing new messages to the queue at scheduled intervals.
 *
 * @constructor Instantiates the TestMessageGenerator using the provided Redis commands object for interacting with Redis.
 *
 * @property commands An instance of RedisCommands used to interact with the Redis database for queuing or retrieving messages.
 */
@Component
class TestMessageGenerator(private val commands: RedisCommands<String, String>)
{
    private val logger = KotlinLogging.logger {}
    private val mapper = ObjectMapper()
    private val dataNameList = ConcurrentHashMap.newKeySet<String>()

    /**
     * Logs the current size of the Redis queue named `benchmarkQueue` to the application logger.
     * This method is scheduled to run at fixed intervals specified by the `@Scheduled` annotation.
     *
     * Functionality:
     * - Retrieves the size of the queue `benchmarkQueue` from the Redis data store.
     * - Outputs the queue size as an informational log message.
     *
     * Scheduling Details:
     * - Initial delay: 10,000 milliseconds (10 seconds) before the method runs for the first time.
     * - Fixed rate: 60,000 milliseconds (1 minute) between consecutive executions.
     *
     * Dependencies:
     * - Accesses the `commands` field to execute Redis operations.
     * - Uses the `logger` field to log the queue size.
     */
    @Scheduled(initialDelay = 10000, fixedRate = 60000)
    private fun logQueueSize()
    {
        val queueSize = commands.llen("benchmarkQueue")
        logger.info { "Current queue size: $queueSize" }
    }

    /**
     * Generates messages and adds them to a Redis queue named `benchmarkQueue`.
     *
     * Functionality:
     * - Checks the current size of the queue using the `llen` command.
     * - Ensures that the queue contains at least 1000 messages.
     * - If the queue size is below the threshold, generates the required number of test messages.
     * - Serializes each generated message into a JSON string and pushes it into the queue using the `lpush` command.
     *
     * Scheduling Details:
     * - The method is scheduled to run periodically.
     * - Initial delay: 5000 milliseconds (5 seconds) before the method executes for the first time.
     * - Fixed rate: 10000 milliseconds (10 seconds) between consecutive executions.
     *
     * Dependencies:
     * - Utilizes the `commands` field to interact with the Redis data store.
     * - Employs the `mapper` field to serialize messages into JSON format.
     * - Uses the `logger` field to log information regarding the message generation process.
     */
    @Scheduled(initialDelay = 5000, fixedRate = 10000)
    private fun generateMessage()
    {
        /* See how many messages are in the queue */
        val queueSize = commands.llen("benchmarkQueue")

        /* If there are less than 1000 messages in the queue, generate as many as needed */
        if (queueSize < 1000) {
            val messagesToGenerate = 1000 - queueSize
            logger.info { "Generating $messagesToGenerate test messages" }

            repeat(messagesToGenerate.toInt())
            {
                val message = mapper.writeValueAsString(generateTestMessage())
                commands.lpush("benchmarkQueue", message)
            }
        }
    }

    /**
     * Generates a test message wrapped in a `MessageWrapper` object. The generated message can be one
     * of the following types: `CreateDataMessage`, `ReadDataMessage`, `UpdateDataMessage`, or `DeleteDataMessage`.
     * The type of message is determined randomly, and specific fields for the message are populated accordingly.
     *
     * Additional behavior:
     * - If the internal `dataNameList` contains fewer than 100 items, a new `CreateDataMessage` is generated.
     * - Otherwise, the message type is selected randomly with varying probabilities.
     *
     * @return A `MessageWrapper` object containing metadata such as the message type, timestamp, and
     *         the corresponding message of type `BaseMessage`.
     */
    private fun generateTestMessage() : MessageWrapper<BaseMessage>
    {
        /* If we don't have any data yet, add some */
        if (dataNameList.size < 100)
        {
            val dataName = UUID.randomUUID().toString()
            dataNameList.add(dataName)
            return MessageWrapper(MessageType.CREATE_MESSAGE, System.currentTimeMillis(), CreateDataMessage(dataName, Random.nextInt(1000, 10000)))
        }

        when (Random.nextInt(0, 100))
        {
            in 0..50 -> {
                /* Most often, we just want to read a random data item */
                /* Get a random data name and create a ReadMessage for it */
                return MessageWrapper(MessageType.READ_MESSAGE, System.currentTimeMillis(), ReadDataMessage(dataNameList.random()))
            }
            in 51..75 -> {
                /* 25% of the time we want to update a random data item */
                /* Get a random data name and create an UpdateMessage for it */
                val randomOriginal = String.format("%02x", Random.nextInt(0, 256))
                val randomReplace = String.format("%02x", Random.nextInt(0, 256))

                return MessageWrapper(MessageType.UPDATE_MESSAGE, System.currentTimeMillis(), UpdateDataMessage(dataNameList.random(), randomOriginal, randomReplace))
            }
            in 76..89 -> {
                /* 25% of the time we want to create a new data item */
                val dataName = UUID.randomUUID().toString()
                dataNameList.add(dataName)
                return MessageWrapper(MessageType.CREATE_MESSAGE, System.currentTimeMillis(), CreateDataMessage(dataName, Random.nextInt(1000, 10000)))

            }
            else -> {
                /* The remaining 14% of the time we want to delete a data item */
                /* Get a random data name and create a DeleteMessage for it */
                return MessageWrapper(MessageType.DELETE_MESSAGE, System.currentTimeMillis(), DeleteDataMessage(dataNameList.random()))
            }
        }
    }
}



