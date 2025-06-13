package data

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.util.concurrent.ThreadPoolExecutor

@Component
class S3DataProviderFactory(
    private val s3Client: S3Client,
    private val threadPoolExecutor: ThreadPoolExecutor)
{
    @Bean
    fun getS3DataProvider(): S3DataProvider
    {
        return S3DataProvider(s3Client, threadPoolExecutor)
    }
}

class S3DataProvider(private val s3Client: S3Client, private val threadPoolExecutor: ThreadPoolExecutor)
{
    private val logger = KotlinLogging.logger {}
    private val bucketName = "nw2s-benchmark-data"

    /**
     * Retrieves a list of object names from an S3 bucket as a list of strings for the filenames
     *
     * The method establishes a request to list objects in a specified S3 bucket,
     * iterates through the result, and collects the keys (object names) into a list.
     *
     * @return A list of strings representing the names of objects in the S3 bucket.
     */
    fun getObjectList() : List<String>
    {
        val objectList = ArrayList<String>()

        val listObjectsRequest = ListObjectsV2Request.builder().bucket(bucketName).build()
        val listStream = s3Client.listObjectsV2(listObjectsRequest).contents().stream()

        listStream.forEach { objectList.add(it.key()) }

        listStream.close()
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
    fun writeDataFile(filename: String, data: String)
    {
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(filename)
            .build()

        val requestBody = RequestBody.fromString(data)

        threadPoolExecutor.submit { s3Client.putObject(putObjectRequest, requestBody) }
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
    fun deleteDataFile(filename: String)
    {
        val deleteObjectRequest = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(filename)
            .build()

        threadPoolExecutor.submit { s3Client.deleteObject(deleteObjectRequest) }
    }
}
