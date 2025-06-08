package application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling

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
@ComponentScan("config", "controllers", "tasks", "listeners")
class Application : SpringBootServletInitializer()

val logger = KotlinLogging.logger {}

fun main(args: Array<String>)
{
	runApplication<Application>(*args)
	logger.info { "Application Started" }
}
