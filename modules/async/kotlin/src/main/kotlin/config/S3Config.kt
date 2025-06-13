package config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import org.springframework.beans.factory.annotation.Value
import software.amazon.awssdk.services.s3.S3Client
import java.util.concurrent.ThreadPoolExecutor

@Configuration
class S3Config()
{
    @Value("\${aws.accessKeyId:}")
    private lateinit var accessKeyId: String

    @Value("\${aws.secretKey:}")
    private lateinit var secretKey: String

    @Value("\${aws.region:us-east-1}")
    private lateinit var region: String

    /**
     * The S3 client seems to need good old-fashioned threads as it blocks when using a singe instance
     * with multiple virtual threads and co-routines.
     */
    private val s3ClientThreadLocal: ThreadLocal<S3Client> = ThreadLocal.withInitial { s3ClientBuilder() }

    @Bean
    fun s3Client(): S3Client
    {
        return s3ClientThreadLocal.get()
    }

    @Bean
    fun s3ClientExecutor(): ThreadPoolExecutor
    {
        return ThreadPoolExecutor(10, 10, 10, java.util.concurrent.TimeUnit.SECONDS, java.util.concurrent.LinkedBlockingQueue())
        {
            runnable -> val thread = Thread(runnable)
            thread.isDaemon = true
            thread
        }
    }

    private fun s3ClientBuilder(): S3Client
    {
        /* If the environment provided access keys, use them */
        /* Otherwise, fall back to using the default credential provider chain */
        if (accessKeyId.isNotEmpty() && secretKey.isNotEmpty())
        {
            val credentials = AwsBasicCredentials.create(accessKeyId, secretKey)

            return S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(region))
                .build()
        }
        else
        {
            return S3Client.builder().region(Region.of(region)).build()
        }
    }
}
