package application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling
import software.amazon.awssdk.services.s3.endpoints.internal.Value
import java.util.concurrent.ConcurrentHashMap

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
@ComponentScan("config", "controllers", "tasks", "listeners", "data", "services")
class Application : SpringBootServletInitializer()
{
    @Bean
	fun getState(): ApplicationState
    {
        return ApplicationState()
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
	var objectList = ConcurrentHashMap.newKeySet<String>()
	var eventCount = 0L
	var eventCountTimeStart = System.currentTimeMillis()
}

