package com.nexters.sseotdabwa.domain.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nexters.sseotdabwa.common.config.AwsProperties;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock
    private AwsProperties props;

    @Mock
    private AwsProperties.S3 s3Props;

    @Mock
    private AwsProperties.Cloudfront cloudfrontProps;

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private S3StorageService s3StorageService;

    @Test
    @DisplayName("presigned PUT 발급 성공 - uploadUrl/s3ObjectKey/viewUrl 반환 + contentType 포함")
    void createPresignedPut_success_includesContentType() throws Exception {
        // given (props wiring)
        given(props.s3()).willReturn(s3Props);
        given(props.cloudfront()).willReturn(cloudfrontProps);

        given(s3Props.bucket()).willReturn("my-bucket");
        given(s3Props.keyPrefix()).willReturn("feeds");
        given(s3Props.presignExpMinutes()).willReturn(5);

        given(cloudfrontProps.domain()).willReturn("https://d111.cloudfront.net");

        // presigned result mock
        PresignedPutObjectRequest presignedMock = mock(PresignedPutObjectRequest.class);
        given(presignedMock.url())
                .willReturn(java.net.URI.create("https://presigned.example.com/put").toURL());

        // presign request 캡쳐
        ArgumentCaptor<PutObjectPresignRequest> presignCaptor =
                ArgumentCaptor.forClass(PutObjectPresignRequest.class);

        given(s3Presigner.presignPutObject(presignCaptor.capture()))
                .willReturn(presignedMock);

        // when
        S3StorageService.PresignedPutResult result =
                s3StorageService.createPresignedPut("test.png", "image/png");

        // then (반환값)
        assertThat(result.uploadUrl()).isEqualTo("https://presigned.example.com/put");
        assertThat(result.s3Key()).startsWith("feeds/");
        assertThat(result.s3Key()).contains("_test.png");
        assertThat(result.viewUrl()).isEqualTo("https://d111.cloudfront.net/" + result.s3Key());

        // then (presign request 내부 검증)
        PutObjectPresignRequest captured = presignCaptor.getValue();
        assertThat(captured.signatureDuration()).isEqualTo(Duration.ofMinutes(5));

        PutObjectRequest putReq = captured.putObjectRequest();
        assertThat(putReq.bucket()).isEqualTo("my-bucket");
        assertThat(putReq.key()).isEqualTo(result.s3Key());
        assertThat(putReq.contentType()).isEqualTo("image/png");

        verify(s3Presigner, times(1)).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    @DisplayName("presigned PUT 발급 - 만료시간 설정이 null이면 기본 10분")
    void createPresignedPut_defaultExpiration_10min() throws Exception {
        // given
        given(props.s3()).willReturn(s3Props);
        given(props.cloudfront()).willReturn(cloudfrontProps);

        given(s3Props.bucket()).willReturn("my-bucket");
        given(s3Props.keyPrefix()).willReturn("feeds");
        given(s3Props.presignExpMinutes()).willReturn(null); // default 10
        given(cloudfrontProps.domain()).willReturn("https://d111.cloudfront.net");

        PresignedPutObjectRequest presignedMock = mock(PresignedPutObjectRequest.class);
        given(presignedMock.url())
                .willReturn(java.net.URI.create("https://presigned.example.com/put").toURL());

        ArgumentCaptor<PutObjectPresignRequest> presignCaptor =
                ArgumentCaptor.forClass(PutObjectPresignRequest.class);

        given(s3Presigner.presignPutObject(presignCaptor.capture()))
                .willReturn(presignedMock);

        // when
        s3StorageService.createPresignedPut("test.jpg", "image/jpeg");

        // then
        PutObjectPresignRequest captured = presignCaptor.getValue();
        assertThat(captured.signatureDuration()).isEqualTo(Duration.ofMinutes(10));
    }


    @Test
    @DisplayName("exists - headObject 성공 시 true")
    void exists_headSuccess_returnsTrue() {
        // given
        given(props.s3()).willReturn(s3Props);
        given(s3Props.bucket()).willReturn("my-bucket");

        // when
        boolean exists = s3StorageService.exists("feeds/ok.jpg");

        // then
        assertThat(exists).isTrue();

        ArgumentCaptor<HeadObjectRequest> captor = ArgumentCaptor.forClass(HeadObjectRequest.class);
        verify(s3Client).headObject(captor.capture());

        HeadObjectRequest req = captor.getValue();
        assertThat(req.bucket()).isEqualTo("my-bucket");
        assertThat(req.key()).isEqualTo("feeds/ok.jpg");
    }

    @Test
    @DisplayName("exists - NoSuchKeyException이면 false")
    void exists_noSuchKey_returnsFalse() {
        // given
        given(props.s3()).willReturn(s3Props);
        given(s3Props.bucket()).willReturn("my-bucket");

        doThrow(NoSuchKeyException.builder().message("no such key").build())
                .when(s3Client).headObject(any(HeadObjectRequest.class));

        // when
        boolean exists = s3StorageService.exists("feeds/missing.jpg");

        // then
        assertThat(exists).isFalse();
    }
}
