package listeners.redis

import application.ApplicationState
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import data.S3DataProvider
import dto.BaseMessage
import dto.CreateDataMessage
import dto.MessageType
import dto.MessageWrapper
import dto.ReadDataMessage
import dto.UpdateDataMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import services.ProcessingService
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

/**
 * Handles the processing of messages received from a Redis queue. This class uses
 * asynchronous processing to handle JSON messages of different types, executing
 * respective actions such as create, read, update, and delete operations.
 *
 * The messages are expected to conform to specific types encoded as JSON strings,
 * and their processing includes deserialization into corresponding message formats.
 * Error scenarios during JSON processing or message handling are logged accordingly.
 *
 * The operations include:
 * - Creating new data and saving it to the datastore.
 * - Reading data from the datastore and logging metadata.
 * - Updating existing data stored in the datastore.
 * - Deleting data from the datastore and updating internal state.
 *
 * This handler also maintains state tracking and logs data-processing latencies.
 *
 * @property objectMapper Used for serializing and deserializing JSON messages.
 * @property dataProvider Responsible for all data storage/retrieval operations from an S3 bucket.
 * @property processingService Handles data generation, transformation, and manipulation logic.
 * @property state Maintains the application's state, including event counts and file lists.
 */
@Component
class RedisQueueHandler(
    private val objectMapper: ObjectMapper,
    private val dataProvider: S3DataProvider,
    private val processingService: ProcessingService,
    private val state: ApplicationState)
{
    val logger = KotlinLogging.logger {}

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
    @Async("asyncExecutor")
    open fun processMessage(message: String)
    {
        try
        {
            /* Deserialize into a base message so we can just get the proper type */
            val typeRef = object : TypeReference<MessageWrapper<BaseMessage>>() {}
            val wrapper = objectMapper.readValue(message, typeRef)
            val latency = System.currentTimeMillis() - wrapper.timestamp
            logger.debug { "Successfully deserialized message with latency: ${ latency}ms" }

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

                    /* Update the list of active files */
                    state.objectList.add(createMessage.message.dataName)

                    logger.debug { "Created new data file ${createMessage.message.dataName} size: ${createMessage.message.dataSize}" }
                    state.eventCount.incrementAndGet()
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

                    logger.debug { "Read file ${readMessage.message.dataName} size: ${lineCount}" }
                    state.eventCount.incrementAndGet()
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
                    data = processingService.updateData(data, updateMessage.message.original, updateMessage.message.replace)
                    dataProvider.writeDataFile(updateMessage.message.dataName, data)

                    logger.debug { "Updated file ${updateMessage.message.dataName} size: ${data.length}" }
                    state.eventCount.incrementAndGet()
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

                    logger.debug { "Deleted file ${deleteMessage.message.dataName}" }
                    state.eventCount.incrementAndGet()
                }
            }
        }
        catch (e: NoSuchKeyException)
        {
            logger.warn(e) { "Failed to find data file: $message" }
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
