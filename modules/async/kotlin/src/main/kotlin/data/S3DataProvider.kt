package data

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.PutObjectRequest

@Component
class S3DataProviderFactory(private val s3Client: S3Client)
{
    @Bean
    fun getS3DataProvider(): S3DataProvider
    {
        return S3DataProvider(s3Client)
    }
}

class S3DataProvider(private val s3Client: S3Client)
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
        s3Client.listObjectsV2(listObjectsRequest).contents().stream().forEach {
            objectList.add(it.key())
        }

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
        val getObjectRequest = software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
            .bucket(bucketName)
            .key(filename)

        val response = s3Client.getObject(getObjectRequest.build())

        return response.readAllBytes().decodeToString()
    }
}
