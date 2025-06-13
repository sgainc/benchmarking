package tasks

import application.ApplicationState
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * A Spring component responsible for logging application statistics at regular intervals.
 *
 * This class serves as a periodic metrics logger, designed to capture and log events per second
 * based on the application's activity. It calculates event frequencies and outputs this information
 * to the application's designated logging system. It is intended to provide insights into the
 * throughput or workload of the system over time.
 *
 * The logging behavior is enabled by the `@Scheduled` annotation, which specifies the initial delay
 * and the fixed interval for execution.
 *
 * @constructor Initializes the Instrumentation component with the given application state.
 *
 * @property state The application state used to track event-related metrics such as event count
 *                 and the timestamp for the start of event counting.
 */
@Component
class Instrumentation(private val state: ApplicationState)
{
    private val logger = KotlinLogging.logger {}

    /**
     * Logs the rate of events processed per second to the application logger.
     *
     * This method calculates the rate of events processed by dividing the total count of events
     * recorded since the last execution by the elapsed time (in seconds). Upon calculation,
     * the event count and timestamp are reset for the next interval. The logged statistics
     * offer insights into the system's event handling performance over time.
     *
     * Functionality:
     * - Retrieves the current event count and resets it to zero.
     * - Calculates the elapsed time since the last execution.
     * - Computes the rate of events processed per second.
     * - Logs the computed rate for monitoring purposes.
     *
     * Dependencies:
     * - Accesses the `state` property of the containing class, which tracks event-related statistics.
     * - Relies on the `logger` property to log the computed rate.
     */
    @Scheduled(initialDelay = 2000, fixedRate = 10000)
    private fun logStats()
    {
        val eventCount = state.eventCount.getAndSet(0)
        val elapsedTime = System.currentTimeMillis() - state.eventCountTimeStart.getAndSet(System.currentTimeMillis())
        val rate = eventCount / (elapsedTime / 1000.0)
        logger.info { "Event count per second: $rate" }
    }
}
