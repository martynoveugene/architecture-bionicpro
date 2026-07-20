package com.bionicpro.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import java.net.URI;

@Configuration
public class S3Config {

    @Value("${S3_URI}")
    private String s3Uri;

    @Value("${S3_USER}")
    private String s3User;

    @Value("${S3_PASSWORD}")
    private String s3Password;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(s3Uri))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3User, s3Password)
                ))
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .build();
    }
}
