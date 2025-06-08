package listeners

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import dto.BaseMessage
import dto.MessageWrapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.json.JsonObject
import jakarta.annotation.PostConstruct
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import kotlin.concurrent.thread


@Component
class RedisQueueListener(
    private val commands: RedisCommands<String, String>,
    private val objectMapper: ObjectMapper)
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
            val typeRef = object : TypeReference<MessageWrapper<BaseMessage>>() {}
            val wrapper = objectMapper.readValue(message, typeRef)
            val latency = System.currentTimeMillis() - wrapper.timestamp
            logger.info { "Successfully deserialized message with latency: ${ latency}ms" }
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
