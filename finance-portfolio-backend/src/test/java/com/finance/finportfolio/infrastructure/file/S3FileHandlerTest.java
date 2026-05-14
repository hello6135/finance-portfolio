package com.finance.finportfolio.infrastructure.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;

@ExtendWith(MockitoExtension.class)
class S3FileHandlerTest {

    @Mock
    private S3Template s3Template;

    private S3FileHandler s3FileHandler;

    // 테스트 데이터 상수
    private static final String BUCKET_NAME = "test-bucket";
    private static final String CLOUDFRONT_DOMAIN = "https://cdn.example.com";

    @BeforeEach
    void setUp() {
        // S3Properties가 record이거나 final일 경우 Mocking 대신 실제 객체 사용
        // 생성자나 빌더를 통해 테스트용 데이터를 직접 주입
        S3Properties s3Properties = new S3Properties(BUCKET_NAME, CLOUDFRONT_DOMAIN);
        s3FileHandler = new S3FileHandler(s3Template, s3Properties);
    }

    @Test
    @DisplayName("파일 업로드 시 S3Template의 upload 메서드가 호출되고 저장 파일명을 반환한다")
    void uploadFileSuccessTest() {
        // given
        String savedFileName = "uuid-test.png";
        String contentType = "image/png";
        MockMultipartFile file = new MockMultipartFile("file", "test.png", contentType, "content".getBytes());

        // when
        String result = s3FileHandler.uploadFile(file, savedFileName, contentType);

        // then
        assertThat(result).isEqualTo(savedFileName);
        verify(s3Template, times(1)).upload(
                eq(BUCKET_NAME),
                eq(savedFileName),
                any(InputStream.class),
                any(ObjectMetadata.class));
    }

    @Test
    @DisplayName("다중 파일 삭제 시 리스트 크기만큼 deleteObject가 호출된다")
    void deleteFilesTest() {
        // given
        List<String> keys = List.of("key1.png", "key2.png");

        // when
        s3FileHandler.deleteFiles(keys);

        // then
        verify(s3Template, times(1)).deleteObject(BUCKET_NAME, "key1.png");
        verify(s3Template, times(1)).deleteObject(BUCKET_NAME, "key2.png");
    }

    @Test
    @DisplayName("클라우드프론트 도메인 정보를 정상적으로 가져온다")
    void getCloudfrontDomainTest() {
        // when
        String result = s3FileHandler.getCloudfrontDomain();

        // then
        assertThat(result).isEqualTo(CLOUDFRONT_DOMAIN);
    }
}