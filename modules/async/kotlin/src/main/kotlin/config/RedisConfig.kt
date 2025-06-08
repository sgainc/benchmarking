package config

import io.lettuce.core.ClientOptions
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.resource.ClientResources
import io.lettuce.core.resource.DefaultClientResources
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * Configuration class for setting up Redis using Lettuce library.
 * This class defines the beans required to connect, interact, and manage
 * Redis server with both synchronous and asynchronous capabilities.
 * It also configures customizable options such as host, port, password,
 * database index, and additional connection settings.
 *
 * The following beans are defined:
 * - `ClientResources`: Configures the client resource pool, including thread pool sizes.
 * - `RedisClient`: Creates and configures the Redis client with URI and options.
 * - `StatefulRedisConnection`: Establishes and maintains a connection to the Redis server.
 * - `RedisAsyncCommands`: Provides access to asynchronous Redis commands.
 * - `RedisCommands`: Provides access to synchronous Redis commands.
 */
@Configuration
class RedisConfig
{
    @Value("\${spring.redis.host:localhost}")
    private lateinit var host: String

    @Value("\${spring.redis.port:6379}")
    private var port: Int = 6379

    @Value("\${spring.redis.password:}")
    private lateinit var password: String

    private var database: Int = 0

    @Bean(destroyMethod = "shutdown")
    fun clientResources(): ClientResources {
        return DefaultClientResources.builder()
            .ioThreadPoolSize(4)
            .computationThreadPoolSize(4)
            .build()
    }

    @Bean
    fun redisClient(clientResources: ClientResources): RedisClient
    {
        val redisURI = RedisURI.builder()
            .withHost(host)
            .withPort(port)
            .apply {
                if (password.isNotEmpty()) {
                    withPassword(password.toCharArray())
                }
            }
            .withDatabase(database)
            .withTimeout(Duration.ofSeconds(2))
            .build()

        return RedisClient.create(clientResources, redisURI).apply {
            options = ClientOptions.builder()
                .autoReconnect(true)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .pingBeforeActivateConnection(true)
                .build()
        }
    }

    @Bean(destroyMethod = "close")
    fun connection(redisClient: RedisClient): StatefulRedisConnection<String, String>
    {
        return redisClient.connect()
    }

    @Bean
    fun asyncCommands(connection: StatefulRedisConnection<String, String>): RedisAsyncCommands<String, String>
    {
        return connection.async()
    }

    @Bean
    fun commands(connection: StatefulRedisConnection<String, String>): RedisCommands<String, String>
    {
        return connection.sync()
    }
}
