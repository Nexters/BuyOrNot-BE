package com.nexters.sseotdabwa.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS SDK v2 Bean 설정.
 *
 * DefaultCredentialsProvider
 *  - 환경변수(AWS KEY)를 자동으로 읽는다.
 *  - 가비아 서버에서는 IAM Role(EC2 metadata)이 없으므로 환경변수로 주입
 */
@Configuration
@EnableConfigurationProperties(AwsProperties.class)
public class AwsClientConfig {

    @Bean
    public S3Client s3Client(AwsProperties props) {
        return S3Client.builder()
                .region(Region.of(props.region()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(AwsProperties props) {
        return S3Presigner.builder()
                .region(Region.of(props.region()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
