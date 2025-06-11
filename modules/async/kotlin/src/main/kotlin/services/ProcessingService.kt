package services

import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.UUID


@Component
class ProcessingServiceFactory(private val processingService: ProcessingService)
{
    @Bean
    fun getProcessingService(): ProcessingService
    {
        return processingService
    }
}


/**
 * A service class providing utility methods for data processing tasks such as generating data
 * and performing string updates.
 */
@Service
class ProcessingService
{
    /**
     * Generates a string by concatenating randomly generated UUID strings a specified number of times.
     * This is used for testing - generating a random bit of data for storage and processing.
     *
     * @param dataSize The number of UUID strings to be concatenated.
     * @return A concatenated string of randomly generated UUIDs.
     */
    fun createData(dataSize: Int): String
    {
        return buildString {
            repeat(dataSize) {
                appendLine(UUID.randomUUID().toString())
            }
        }
    }

    /**
     * Updates the given source string by replacing instances of the original substring with the replacement substring.
     *
     * @param source The original string to be updated.
     * @param original The substring within the source string that needs to be replaced.
     * @param replace The substring that will replace the original substring in the source string.
     * @return A new string with the original substring replaced by the replacement substring.
     */
    fun updateData(source: String, original: String, replace: String): String
    {
        return source.replace(original, replace)
    }
}
