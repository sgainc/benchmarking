package tasks

import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.api.async.RedisAsyncCommands
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import java.time.LocalDateTime
import java.util.UUID

@Component
class TestMessageGenerator(private val asyncCommands: RedisAsyncCommands<String, String>)
{
    private val logger = KotlinLogging.logger {}

    @Scheduled(initialDelay = 5000, fixedRate = 10000)
    fun generateMessage()
    {
        logger.info { "Generating test message" }
        val message = "ZZ Test message ${UUID.randomUUID()} generated at ${LocalDateTime.now()}"

        asyncCommands.lpush("benchmarkQueue", message)
    }
}



