package com.finance.finportfolio.infrastructure.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.finance.finportfolio.global.error.exception.FileStorageException;

import io.awspring.cloud.s3.Location;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Resource;
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
    @DisplayName("파일 업로드 중 IOException 발생 시 FileStorageException을 던진다")
    void uploadFileIOExceptionTest() {
        // given
        String savedFileName = "uuid-test.png";
        String contentType = "image/png";

        // MockMultipartFile의 getInputStream() 호출 시 IOException을 유도하기 위해 스파이 또는 익명 클래스
        // 활용 가능
        // 혹은 S3Template.upload에서 내부적으로 스트림 읽다 터지는 상황 모킹
        MockMultipartFile file = new MockMultipartFile("file", "test.png", contentType, "content".getBytes()) {
            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("인위적 파일 읽기 실패");
            }
        };

        // when & then
        assertThatThrownBy(() -> s3FileHandler.uploadFile(file, savedFileName, contentType))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("S3 파일 업로드 중 오류 발생")
                .hasCauseInstanceOf(IOException.class);
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

    @Test
    @DisplayName("S3 오브젝트 키 목록을 정상적으로 조회하고 null이나 빈 값은 필터링한다")
    void getS3ObjectKeysSuccessTest() {
        // given
        S3Resource resource1 = mock(S3Resource.class);
        S3Resource resource2 = mock(S3Resource.class);
        S3Resource resource3 = mock(S3Resource.class); // null 필터링 테스트용
        S3Resource resource4 = mock(S3Resource.class); // 빈 문자열 필터링 테스트용

        Location location1 = mock(Location.class);
        Location location2 = mock(Location.class);
        Location location3 = mock(Location.class);
        Location location4 = mock(Location.class);

        // 각 가짜 리소스가 가짜 Location 객체를 반환하도록 설정
        when(resource1.getLocation()).thenReturn(location1);
        when(location1.getObject()).thenReturn("images/photo1.png");

        when(resource2.getLocation()).thenReturn(location2);
        when(location2.getObject()).thenReturn("images/photo2.png");

        // Objects::nonNull 검증용 가짜 스텁 설정
        when(resource3.getLocation()).thenReturn(location3);
        when(location3.getObject()).thenReturn(null);

        // !filename.isEmpty() 검증용 가짜 스텁 설정
        when(resource4.getLocation()).thenReturn(location4);
        when(location4.getObject()).thenReturn("");

        // S3Template Mock이 위에서 가공된 4개의 리소스를 반환하도록 리스트 지정
        List<S3Resource> mockResources = List.of(resource1, resource2, resource3, resource4);
        when(s3Template.listObjects(BUCKET_NAME, "")).thenReturn(mockResources);

        // when
        List<String> result = s3FileHandler.getS3ObjectKeys();

        // then
        assertThat(result)
                .hasSize(2)
                .containsExactly("images/photo1.png", "images/photo2.png");
        verify(s3Template, times(1)).listObjects(BUCKET_NAME, "");
    }
}