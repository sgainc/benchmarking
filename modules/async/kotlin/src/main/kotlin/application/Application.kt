package application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.time.Duration
import java.util.concurrent.ThreadPoolExecutor

/**
 * The main entry point for the Spring Boot application.
 *
 * This class serves as the configuration and bootstrap point for the application.
 * It is annotated with essential Spring Boot annotations to enable specific functionalities:
 *
 * - `@SpringBootApplication`: Facilitates auto-configuration, component scanning, and enables Spring Boot application features.
 * - `@EnableScheduling`: Enables scheduling support in the application.
 * - `@ComponentScan`: Explicitly scans the specified packages for components,configurations, and Spring-managed beans.
 *
 * It extends `SpringBootServletInitializer`, which allows the application
 * to be initialized and deployed in a traditional servlet container.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@ComponentScan("config", "controllers", "tasks", "listeners", "data", "services")
class Application : SpringBootServletInitializer()
{
	@Value("\${async.maxPoolSize:1}")
	private var maxPoolSize : Int = 1

	@Value("\${async.corePoolSize:1}")
	private var corePoolSize : Int = 1

	@Value("\${async.queueCapacity:0}")
	private var queueCapacity : Int = 0

	@Value("\${async.keepAliveSeconds:0}")
	private var keepAliveSeconds : Int = 0

	@Value("\${async.threadNamePrefix:async-thread-}")
	private var threadNamePrefix : String = "async-thread-"


    @Bean
	fun getState(): ApplicationState
    {
        return ApplicationState()
    }

	/**
	 * Creates and configures a thread pool executor for asynchronous processing.
	 * These threads are used by the @Async annotation to process tasks asynchronously
	 * at the methodName level.
	 */
	@Bean
	fun asyncExecutor() : TaskExecutor
	{
		val executor = ThreadPoolTaskExecutorBuilder()
			.corePoolSize(corePoolSize)
			.maxPoolSize(maxPoolSize)
			.queueCapacity(queueCapacity)
			.keepAlive(Duration.ofSeconds(keepAliveSeconds.toLong()))
			.threadNamePrefix(threadNamePrefix)
			.build()

		executor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())

		return executor
	}

	companion object {

		private val logger = KotlinLogging.logger {}

		@JvmStatic
		fun main(args: Array<String>) {
			runApplication<Application>(*args)
			logger.info { "Application Started" }
		}
	}
}

class ApplicationState
{
	/**
	 * A set of strings representing the names of objects currently being processed by the application.
	 * This set is used to track the progress of data processing and is updated throughout the application.
	 *
	 * This property is thread-safe and can be accessed from any thread.
	 */
	var objectList = ConcurrentHashMap.newKeySet<String>()

	/**
	 * A counter used to track the total number of events processed by the application.
	 * This counter is updated throughout the application and can be accessed from any thread.
	 */
	@Volatile
	var eventCount : AtomicLong = AtomicLong(0)

	/**
	 * A counter used to track the total time taken to process events by the application.
	 * This counter is updated throughout the application and can be accessed from any thread.
	 * Along with the eventCount, this value can be used to calculate the average throughput of the application..
	 */
	@Volatile
	var eventCountTimeStart : AtomicLong = AtomicLong(System.currentTimeMillis())
}
