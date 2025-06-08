package controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * A REST controller for managing test endpoints.
 *
 * This controller handles requests sent to the "/test" base URL and
 * provides utility functions, such as checking the application's health status.
 */
@RestController
@RequestMapping("/test")
class TestController
{
    private val logger = KotlinLogging.logger {}

    @GetMapping("/isup")
    fun isup() : Boolean
    {
        logger.info { "Test Request Completed" }

        return true
    }
}
