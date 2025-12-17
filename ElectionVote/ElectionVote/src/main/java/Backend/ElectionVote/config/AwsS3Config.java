package Backend.ElectionVote.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;

/**
 * Creates S3Client and S3Presigner beans using the default credentials provider chain.
 * Make sure you set app.storage.bucket and AWS credentials/role in your environment or IAM.
 */
@Configuration
public class AwsS3Config {

    @Value("${cloud.aws.region:us-east-1}")
    private String awsRegion;

    @Bean
    public Region awsRegion() {
        return Region.of(awsRegion);
    }

    @Bean
    public S3Client s3Client(Region awsRegion) {
        return S3Client.builder()
                .region(awsRegion)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallAttemptTimeout(Duration.ofSeconds(120))
                        .build())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(Region awsRegion) {
        return S3Presigner.builder()
                .region(awsRegion)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}