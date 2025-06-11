package config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import org.springframework.beans.factory.annotation.Value

@Configuration
class S3Config()
{
    @Value("\${aws.accessKeyId:}")
    private lateinit var accessKeyId: String

    @Value("\${aws.secretKey:}")
    private lateinit var secretKey: String

    @Value("\${aws.region:us-east-1}")
    private lateinit var region: String

    @Bean
    fun s3Client(): S3Client
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
