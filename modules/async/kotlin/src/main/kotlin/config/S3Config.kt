package config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder
import org.springframework.core.task.TaskExecutor
import software.amazon.awssdk.services.s3.S3Client
import java.time.Duration
import java.util.concurrent.ThreadPoolExecutor

@Configuration
class S3Config()
{



}
