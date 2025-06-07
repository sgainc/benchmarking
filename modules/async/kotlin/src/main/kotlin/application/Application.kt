package application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling

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
