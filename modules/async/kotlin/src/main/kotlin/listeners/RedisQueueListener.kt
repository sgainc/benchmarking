package listeners

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.JsonProcessingException
import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.json.JsonObject
import jakarta.annotation.PostConstruct
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class QueueMessageEventListener(
    private val asyncCommands: RedisAsyncCommands<String, String>,
    private val objectMapper: ObjectMapper)
{
    val logger = KotlinLogging.logger {}
    private val queueKey = "benchmarkQueue"
    private val pollingDelayMillis = 100L

    @PostConstruct
    fun startListening()
    {
//        consumeMessages()
//            .subscribe(
//                { message -> logger.info { "Processed message: $message" } },
//                { error -> logger.error(error) { "Error processing queue" } }
//            )
    }

//    private fun consumeMessages(): Flux<String>
//    {
//        return Flux.defer {
//            reactiveRedisTemplate.opsForList()
//                .rightPop(queueKey)
//                .repeatWhenEmpty(Int.MAX_VALUE) { it.delayElements(Duration.ofMillis(pollingDelayMillis)) }
//        }.doOnNext { message ->
//            processMessage(message)
//        }.onErrorContinue { error, _ ->
//            logger.error(error) { "Error processing message from queue $queueKey" }
//        }
//    }

    private fun processMessage(message: String)
    {
        try
        {
            val jsonNode = objectMapper.readTree(message)
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
