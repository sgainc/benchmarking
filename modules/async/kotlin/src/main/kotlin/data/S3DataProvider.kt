package data

import application.ApplicationState
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.time.Duration
import java.util.concurrent.ThreadPoolExecutor

@Component
class S3DataProviderFactory(private val state: ApplicationState)
{
    private val s3ClientThreadLocal: ThreadLocal<S3Client> = ThreadLocal.withInitial { s3ClientBuilder() }
    private val s3DataProviderThreadLocal: ThreadLocal<S3DataProvider> = ThreadLocal.withInitial { S3DataProvider( s3ClientThreadLocal.get()) }

    @Value("\${aws.accessKeyId:}")
    private lateinit var accessKeyId: String

    @Value("\${aws.secretKey:}")
    private lateinit var secretKey: String

    @Value("\${aws.region:us-east-1}")
    private lateinit var region: String

    @Value("\${s3writer.maxPoolSize:1}")
    private var maxPoolSize : Int = 1

    @Value("\${s3writer.corePoolSize:1}")
    private var corePoolSize : Int = 1

    @Value("\${s3writer.queueCapacity:0}")
    private var queueCapacity : Int = 0

    @Value("\${s3writer.keepAliveSeconds:0}")
    private var keepAliveSeconds : Long = 0

    @Value("\${async.threadNamePrefix:s3-writer-}")
    private var threadNamePrefix : String = "s3-writer-"

    private val logger = KotlinLogging.logger {}


    @Bean
    fun getS3DataProvider(): S3DataProvider
    {
        return s3DataProviderThreadLocal.get()
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

    @Bean
    private fun asyncS3WriterTaskExecutor() : TaskExecutor
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
}

open class S3DataProvider(val s3Client: S3Client)
{
    private val bucketName = "nw2s-benchmark-data"

    /**
     * Retrieves a list of object names from an S3 bucket as a list of strings for the filenames
     *
     * The method establishes a request to list objects in a specified S3 bucket,
     * iterates through the result, and collects the keys (object names) into a list.
     *
     * @return A list of strings representing the names of objects in the S3 bucket.
     */
    open fun getObjectList() : List<String>
    {
        val objectList = ArrayList<String>()

        do
        {
            val listObjectsRequest = ListObjectsV2Request.builder().bucket(bucketName).build()
            val listResponse = s3Client.listObjectsV2(listObjectsRequest)

            listResponse.contents().stream().forEach { objectList.add(it.key()) }

            /* If we are done, don't forget to close! */
            if (!listResponse.isTruncated)
            {
                listResponse.contents().stream().close()
            }
        }
        while(listResponse.isTruncated)

        return objectList
    }

    /**
     * Writes a data file to the S3 bucket with the specified filename and content.
     * Specifically used for testing throughput of a queue processor. The data should
     * consist only of a long string that will get written to the file as-is.
     *
     * @param filename The name of the file to be written to the S3 bucket.
     * @param data The content to be written in the file.
     */
    @Async("asyncS3WriterTaskExecutor")
    open fun writeDataFile(filename: String, data: String)
    {
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(filename)
            .build()

        val requestBody = RequestBody.fromString(data)
        s3Client.putObject(putObjectRequest, requestBody)
    }

    /**
     * Reads a data file from the S3 bucket with the specified filename.
     * Specifically used for testing throughput of a queue processor where the content
     * will be read as a text string.
     *
     * @param filename The name of the file to be read from the S3 bucket.
     * @return The content of the file as a string.
     */
    fun getDataFile(filename: String): String
    {
        val getObjectRequest = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(filename)

        val response = s3Client.getObject(getObjectRequest.build())
        val result = response.readAllBytes().decodeToString()
        response.close()

        return result
    }

    /**
     * Deletes a data file from the S3 bucket with the specified filename.
     * This method initiates a request to remove the object from the S3 bucket and
     * processes the deletion.
     *
     * @param filename The name of the file to be deleted from the S3 bucket.
     */
    @Async("asyncS3WriterTaskExecutor")
    open fun deleteDataFile(filename: String)
    {
        val deleteObjectRequest = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(filename)
            .build()

        s3Client.deleteObject(deleteObjectRequest)
    }
}
