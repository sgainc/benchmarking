package tasks

import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.api.sync.RedisCommands
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import java.time.LocalDateTime
import java.util.UUID

@Component
class TestMessageGenerator(private val commands: RedisCommands<String, String>)
{
    private val logger = KotlinLogging.logger {}

    @Scheduled(initialDelay = 5000, fixedRate = 10000)
    fun generateMessage()
    {
        /* See how many messages are in the queue */
        val queueSize = commands.llen("benchmarkQueue")

        /* If there are less than 1000 messages in the queue, generate as many as needed */
        if (queueSize < 1000) {
            val messagesToGenerate = 1000 - queueSize
            logger.info { "Generating $messagesToGenerate test messages" }

            repeat(messagesToGenerate.toInt())
            {
                val message = "ZZ Test message ${UUID.randomUUID()} generated at ${LocalDateTime.now()}"
                commands.lpush("benchmarkQueue", message)
            }
        }
    }
}



