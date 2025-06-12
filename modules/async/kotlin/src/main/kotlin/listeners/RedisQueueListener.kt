package listeners

import application.ApplicationState
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import data.S3DataProvider
import dto.BaseMessage
import dto.CreateDataMessage
import dto.MessageType
import dto.MessageWrapper
import dto.ReadDataMessage
import dto.UpdateDataMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.api.sync.RedisCommands
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import services.ProcessingService
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
    private val objectMapper: ObjectMapper,
    private val dataProvider: S3DataProvider,
    private val processingService: ProcessingService,
    private val state: ApplicationState
)
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
                    //TODO: getting some key does not exist errors.
                    /* Using Java Loom to spinup threads for handling the messages */
                    //Thread.ofVirtual().start() { processMessage(message.value) }
                    processMessage(message.value)
                }
            }
        }
    }

    /**
     * Processes a JSON message received from the Redis queue.
     * The method deserializes the JSON message into an appropriate message type
     * and executes the corresponding action based on the message type.
     *
     * The message types handled include:
     * - `CREATE_MESSAGE`: Creates new data and stores it in the datastore.
     * - `READ_MESSAGE`: Reads data from the datastore and logs the line count.
     * - `UPDATE_MESSAGE`: Updates existing data in the datastore.
     * - `DELETE_MESSAGE`: Deletes data from the datastore and updates the internal state.
     *
     * The method also calculates and logs the latency of message processing.
     *
     * Errors during deserialization or processing are logged.
     *
     * @param message A JSON-formatted string representing a message to be processed.
     */
    private fun processMessage(message: String)
    {
        try
        {
            /* Deserialize into a base message so we can just get the proper type */
            val typeRef = object : TypeReference<MessageWrapper<BaseMessage>>() {}
            val wrapper = objectMapper.readValue(message, typeRef)
            val latency = System.currentTimeMillis() - wrapper.timestamp
            logger.info { "Successfully deserialized message with latency: ${ latency}ms" }

            /* Check the message type and deserialize into the correct lower level type */
            when (wrapper.messageType)
            {
                MessageType.CREATE_MESSAGE ->
                {
                    val typeRef = object : TypeReference<MessageWrapper<CreateDataMessage>>() {}
                    val createMessage = objectMapper.readValue(message, typeRef)

                    /* Generate the data and save to data store */
                    val data = processingService.createData(createMessage.message.dataSize)
                    dataProvider.writeDataFile(createMessage.message.dataName, data)

                    logger.info { "Created new data file ${createMessage.message.dataName} size: ${createMessage.message.dataSize}" }
                }
                MessageType.READ_MESSAGE ->
                {
                    /* Revalidate that the entry still exists. If not log a note and move along. */
                    if (!state.objectList.contains(wrapper.message.dataName))
                    {
                        logger.info { "Entry ${wrapper.message.dataName} no longer exists, skipping update" }
                        return;
                    }

                    val typeRef = object : TypeReference<MessageWrapper<ReadDataMessage>>() {}
                    val readMessage = objectMapper.readValue(message, typeRef)

                    /* Read the message data and count how many lines we got */
                    val data = dataProvider.getDataFile(readMessage.message.dataName)
                    val lineCount = data.count { it == '\n' }

                    logger.info { "Read file ${readMessage.message.dataName} size: ${lineCount}" }
                }
                MessageType.UPDATE_MESSAGE ->
                {
                    /* Revalidate that the entry still exists. If not log a note and move along. */
                    if (!state.objectList.contains(wrapper.message.dataName))
                    {
                        logger.info { "Entry ${wrapper.message.dataName} no longer exists, skipping update" }
                        return;
                    }

                    val typeRef = object : TypeReference<MessageWrapper<UpdateDataMessage>>() {}
                    val updateMessage = objectMapper.readValue(message, typeRef)

                    /* Read the file into a string, process the string, and then resave the file */
                    var data = dataProvider.getDataFile(updateMessage.message.dataName)
                    data = processingService.updateData(wrapper.message.dataName, updateMessage.message.original, updateMessage.message.replace)
                    dataProvider.writeDataFile(updateMessage.message.dataName, data)

                    logger.info { "Updated file ${updateMessage.message.dataName} size: ${data.length}" }
                }
                MessageType.DELETE_MESSAGE ->
                {
                    /* Revalidate that the entry still exists. If not log a note and move along. */
                    if (!state.objectList.contains(wrapper.message.dataName))
                    {
                        logger.info { "Entry ${wrapper.message.dataName} no longer exists, skipping update" }
                        return;
                    }

                    /* delete this entry from the list of active files */
                    state.objectList.remove(wrapper.message.dataName)

                    val typeRef = object : TypeReference<MessageWrapper<BaseMessage>>() {}
                    val deleteMessage = objectMapper.readValue(message, typeRef)

                    /* Then delete from data store */
                    dataProvider.deleteDataFile(deleteMessage.message.dataName)

                    logger.info { "Deleted file ${deleteMessage.message.dataName}" }
                }
            }
        }
        catch (e: JsonProcessingException)
        {
            logger.error(e) { "Failed to parse message as JSON: $message" }
        }
        catch (e: Exception)
        {
            logger.error(e) { "Failed to process message: $message" }
        }
    }
}
