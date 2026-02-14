package com.nexters.sseotdabwa.domain.storage.service;

import com.nexters.sseotdabwa.common.config.AwsProperties;
import com.nexters.sseotdabwa.common.exception.GlobalException;

import com.nexters.sseotdabwa.domain.storage.exception.StorageErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * S3 presigned URL 생성 + S3 삭제 + CloudFront 조회 URL 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final AwsProperties props;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    /**
     * presigned PUT URL 생성
     *
     * @param originalFileName 클라이언트 원본 파일명 (key 생성에 사용)
     * @param contentType      ex: image/jpeg
     * @return uploadUrl, s3Key, viewUrl
     *
     */

    // 허용 contentType allowlist
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/heic",
            "image/gif"
    );

    public PresignedPutResult createPresignedPut(String originalFileName, String contentType) {
        // ContentType 검증 (Stored XSS 방지)
        validateContentType(contentType);

        String bucket = props.s3().bucket();
        String s3Key = buildS3Key(props.s3().keyPrefix(), originalFileName);

        // 1) 서명에 포함될 PutObjectRequest 구성
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(normalizeContentType(contentType))
                .build();

        // 2) 만료 시간 설정 (기본 10분)
        int expMin = props.s3().presignExpMinutes() != null ? props.s3().presignExpMinutes() : 10;

        PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expMin))
                .putObjectRequest(putReq)
                .build();

        // 3) presigned url 생성
        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignReq);

        // 4) CloudFront 조회 URL 생성
        String viewUrl = buildCloudFrontUrl(props.cloudfront().domain(), s3Key);

        log.info("Created presigned PUT url. key={}, expires={}min", s3Key, expMin);

        return new PresignedPutResult(
                presigned.url().toString(),
                s3Key,
                viewUrl
        );
    }

    // contentType normalize + allowlist 검증
    private void validateContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            throw new GlobalException(StorageErrorCode.CONTENT_TYPE_REQUIRED);
        }
        String normalized = normalizeContentType(contentType);
        if (!ALLOWED_CONTENT_TYPES.contains(normalized)) {
            throw new GlobalException(StorageErrorCode.UNSUPPORTED_CONTENT_TYPE);
        }
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? null : contentType.trim().toLowerCase();
    }

    /**
     * 업로드 완료 확인용 headObject
     */
    public boolean exists(String s3Key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(props.s3().bucket())
                    .key(s3Key)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.warn("headObject failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * S3 오브젝트 삭제
     */
    public void deleteObject(String s3Key) {
        DeleteObjectRequest deleteReq = DeleteObjectRequest.builder()
                .bucket(props.s3().bucket())
                .key(s3Key)
                .build();

        s3Client.deleteObject(deleteReq);
        log.info("Deleted S3 object. key={}", s3Key);
    }

    /**
     * s3Key 생성 규칙
     * - {prefix}/{uuid}_{originalFileName}
     * - 파일명에 경로 문자(/, \)가 들어오면 key 오염 가능 → 제거/치환
     * - 공백/이상 문자 최소 정리
     */
    private String buildS3Key(String prefix, String originalFileName) {
        String safeName = (originalFileName == null || originalFileName.isBlank())
                ? "file"
                : originalFileName.trim();

        // 경로 문자 제거 → key 오염 방지
        safeName = safeName.replaceAll("[/\\\\]", "_");

        // 연속 공백/특수문자 정리
        safeName = safeName.replaceAll("\\s+", "_");

        String uuid = UUID.randomUUID().toString().replace("-", "");
        String fileName = uuid + "_" + safeName;

        if (StringUtils.hasText(prefix)) {
            String p = prefix.replaceAll("^/+", "").replaceAll("/+$", "");
            return p + "/" + fileName;
        }
        return fileName;
    }

    /**
     * CloudFront는 public이므로 단순 조합으로 조회 URL 생성
     * - domain: img.buy-or-not.com/
     * - s3Key: feeds/uuid_xxx.jpg
     */
    private String buildCloudFrontUrl(String domain, String s3Key) {
        String d = domain;
        if (d.endsWith("/")) d = d.substring(0, d.length() - 1);
        return d + "/" + s3Key;
    }

    public record PresignedPutResult(
            String uploadUrl,
            String s3Key,
            String viewUrl
    ) {}
}
