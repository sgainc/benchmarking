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

@Component
class TestMessageGenerator(private val commands: RedisCommands<String, String>)
{
    private val logger = KotlinLogging.logger {}
    private val mapper = ObjectMapper()
    private val dataNameList = ConcurrentHashMap.newKeySet<String>()

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
                /* Get a random data name and create a ReadMessage for it */
                return MessageWrapper(MessageType.READ_MESSAGE, System.currentTimeMillis(), ReadDataMessage(dataNameList.random()))
            }
            in 51..75 -> {
                /* Get a random data name and create an UpdateMessage for it */
                val randomOriginal = String.format("%02x", Random.nextInt(0, 256))
                val randomReplace = String.format("%02x", Random.nextInt(0, 256))

                return MessageWrapper(MessageType.UPDATE_MESSAGE, System.currentTimeMillis(), UpdateDataMessage(dataNameList.random(), randomOriginal, randomReplace))
            }
            in 76..89 -> {
                val dataName = UUID.randomUUID().toString()
                dataNameList.add(dataName)
                return MessageWrapper(MessageType.CREATE_MESSAGE, System.currentTimeMillis(), CreateDataMessage(dataName, Random.nextInt(1000, 10000)))

            }
            else -> {
                return MessageWrapper(MessageType.DELETE_MESSAGE, System.currentTimeMillis(), DeleteDataMessage(dataNameList.random()))
            }
        }
    }
}



