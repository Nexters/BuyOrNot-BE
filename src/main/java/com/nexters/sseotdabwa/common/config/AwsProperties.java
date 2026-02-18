package com.nexters.sseotdabwa.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 aws.* 설정을 타입 안전하게 바인딩하기 위한 record.
 * - credentials는 넣지 않는다 (환경변수로만 주입).
 */
@ConfigurationProperties(prefix = "aws")
public record AwsProperties(
        String region,
        S3 s3,
        Cloudfront cloudfront
) {
    public record S3(
            String bucket,
            String keyPrefix,
            Integer presignExpMinutes,
            Long maxBytes
    ) {}

    public record Cloudfront(
            String domain
    ) {}
}
