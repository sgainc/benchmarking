package listeners

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
import io.lettuce.core.json.JsonObject
import jakarta.annotation.PostConstruct
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
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
    private val processingService: ProcessingService
)
{
    val logger = KotlinLogging.logger {}
    private val queueKey = "benchmarkQueue"

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
                    processMessage(message.value)
                }
            }
        }
    }

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
                    val typeRef = object : TypeReference<MessageWrapper<ReadDataMessage>>() {}
                    val readMessage = objectMapper.readValue(message, typeRef)

                    /* Read the message data and count how many lines we got */
                    val data = dataProvider.getDataFile(readMessage.message.dataName)
                    val lineCount = data.count { it == '\n' }

                    logger.info { "Read file size: ${lineCount}" }
                }
                MessageType.UPDATE_MESSAGE ->
                {
                    val typeRef = object : TypeReference<MessageWrapper<UpdateDataMessage>>() {}
                    val updateMessage = objectMapper.readValue(message, typeRef)

                    /* Read the file into a string, process the string, and then resave the file */
                    var data = dataProvider.getDataFile(updateMessage.message.dataName)
                    data = processingService.updateData(wrapper.message.dataName, updateMessage.message.original, updateMessage.message.replace)
                    dataProvider.writeDataFile(updateMessage.message.dataName, data)

                    logger.info { "Updated file size: ${data.length}" }
                }
                MessageType.DELETE_MESSAGE ->
                {
                    //TODO: delete from data list
                    val typeRef = object : TypeReference<MessageWrapper<BaseMessage>>() {}
                    val deleteMessage = objectMapper.readValue(message, typeRef)

                    /* Just delete the file and remove from our local list */
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

    @Async
    @EventListener
    fun handleQueueMessage(event: JsonObject)
    {
        try
        {
            logger.info { "Processing message: $event" }

        }
        catch (e: Exception)
        {
            logger.error(e) { "Error processing message: $event" }
        }
    }
}
