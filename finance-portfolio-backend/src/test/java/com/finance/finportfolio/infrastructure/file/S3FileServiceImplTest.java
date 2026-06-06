package com.finance.finportfolio.infrastructure.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.tika.Tika;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class S3FileServiceImplTest {

    @Mock
    private S3FileHandler s3FileHandler;

    @Mock
    private Tika tika;

    @InjectMocks
    private S3FileServiceImpl s3FileService;

    @Nested
    @DisplayName("파일 업로드 테스트")
    class UploadFile {

        @Test
        @DisplayName("정상적인 이미지 파일일 경우 업로드에 성공한다")
        void uploadFileSuccessTest() throws IOException {
            // given
            // java.awt를 사용하여 Imaging 라이브러리가 완벽히 인식하는 가상의 1x1 PNG 바이트 배열 동적 생성
            java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(1, 1,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(bufferedImage, "png", baos);
            byte[] validPngBytes = baos.toByteArray();

            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.png", "image/png", validPngBytes);

            given(tika.detect(any(InputStream.class))).willReturn("image/png");
            given(s3FileHandler.uploadFile(eq(file), anyString(), eq("image/png")))
                    .willReturn("uuid-test.png");

            // when
            String result = s3FileService.uploadFile(file);

            // then
            assertThat(result).isEqualTo("uuid-test.png");
            verify(s3FileHandler, times(1)).uploadFile(eq(file), anyString(), eq("image/png"));
        }

        @Test
        @DisplayName("파일이 없거나 비어있는 경우 예외를 발생시킨다")
        void uploadFileEmptyExceptionTest() {
            // given
            MockMultipartFile emptyFile = new MockMultipartFile("file", "", "image/png", new byte[0]);

            // when & then
            assertThatThrownBy(() -> s3FileService.uploadFile(emptyFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("업로드할 파일이 없습니다.");
        }

        @Test
        @DisplayName("용량이 제한(1MB)을 초과하는 경우 예외를 발생시킨다")
        void uploadFileLargeSizeExceptionTest() {
            // given
            byte[] largeBytes = new byte[1024 * 1024 + 1]; // 1MB + 1Byte
            MockMultipartFile largeFile = new MockMultipartFile("file", "large.png", "image/png", largeBytes);

            // when & then
            assertThatThrownBy(() -> s3FileService.uploadFile(largeFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("파일 용량이 너무 큽니다.");
        }

        @Test
        @DisplayName("MIME 타입이 이미지가 아닐 경우 예외를 발생시킨다")
        void uploadFileInvalidMimeTypeExceptionTest() throws IOException {
            // given
            MockMultipartFile textFile = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());
            given(tika.detect(any(InputStream.class))).willReturn("text/plain");

            // when & then
            assertThatThrownBy(() -> s3FileService.uploadFile(textFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("허용되지 않는 파일 형식입니다.");
        }

        @Test
        @DisplayName("실제 파일 바이너리 형식(MIME)과 확장자가 일치하지 않으면 예외를 발생시킨다")
        void uploadFileMimeAndExtensionMismatchExceptionTest() throws IOException {
            // given: PNG 바이트를 가졌지만 파일명은 test.jpg인 경우
            java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(1, 1,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(bufferedImage, "png", baos);
            byte[] validPngBytes = baos.toByteArray();

            MockMultipartFile mismatchFile = new MockMultipartFile(
                    "file", "test.jpg", "image/png", validPngBytes);

            given(tika.detect(any(InputStream.class))).willReturn("image/png");

            // when & then
            assertThatThrownBy(() -> s3FileService.uploadFile(mismatchFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("실제 파일 형식과 확장자가 일치하지 않습니다.");
        }

        @Test
        @DisplayName("파일에 확장자가 없는 경우 예외를 발생시킨다")
        void uploadFileNoExtensionExceptionTest() throws IOException {
            // given
            java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(1, 1,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(bufferedImage, "png", baos);
            byte[] validPngBytes = baos.toByteArray();

            MockMultipartFile noExtFile = new MockMultipartFile(
                    "file", "noextension", "image/png", validPngBytes);

            given(tika.detect(any(InputStream.class))).willReturn("image/png");

            // when & then
            assertThatThrownBy(() -> s3FileService.uploadFile(noExtFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("파일 확장자가 올바르지 않습니다.");
        }

        @Test
        @DisplayName("파일명이 비어있거나 올바르지 않은 경우 예외를 발생시킨다")
        void uploadFileBlankFilenameExceptionTest() throws IOException {
            // given
            java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(1, 1,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(bufferedImage, "png", baos);
            byte[] validPngBytes = baos.toByteArray();

            MockMultipartFile blankNameFile = new MockMultipartFile(
                    "file", "   ", "image/png", validPngBytes);

            given(tika.detect(any(InputStream.class))).willReturn("image/png");

            // when & then
            assertThatThrownBy(() -> s3FileService.uploadFile(blankNameFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("올바르지 않은 파일명입니다.");
        }
    }

    @Nested
    @DisplayName("HTML 본문 파일 처리 테스트")
    class ContentFileProcessing {

        @Test
        @DisplayName("본문 데이터 삭제 시 포함된 이미지 파일의 키만 추출하여 S3Handler 다중삭제를 호출한다")
        void deleteFilesTest() {
            // given
            String uuid1 = "12345678-1234-1234-1234-123456781234";
            String uuid2 = "abcdefab-abcd-abcd-abcd-abcdefabcdef";
            String content = "<p>본문내용</p><img src=\"" + uuid1 + "-img1.png\"><img src=\"https://cdn.example.com/"
                    + uuid2 + "-img2.png\">";

            // when
            s3FileService.deleteFiles(content);

            // then
            verify(s3FileHandler, times(1)).deleteFiles(List.of(uuid1 + "-img1.png", uuid2 + "-img2.png"));
        }

        @Test
        @DisplayName("본문을 조회용으로 전환 시 도메인이 없는 파일명에 CDN 주소를 붙여준다")
        void convertToCdnUrlsTest() {
            // given
            String pureKey = "12345678-1234-1234-1234-123456781234-image.png";
            String content = "<img src=\"" + pureKey + "\">";
            given(s3FileHandler.getCloudfrontDomain()).willReturn("cdn.financeportfolio.com");

            // when
            String result = s3FileService.convertToCdnUrls(content);

            // then
            assertThat(result).contains("https://cdn.financeportfolio.com/" + pureKey);
        }

        @Test
        @DisplayName("본문을 저장용으로 전환 시 CDN 주소를 떼고 HTML 살균(Jsoup.clean)을 수행한다")
        void removeCdnUrlsTest() {
            // given
            String pureKey = "12345678-1234-1234-1234-123456781234-image.png";
            String inputHtml = "<p>안전한 본문</p><img src=\"https://cdn.example.com/" + pureKey
                    + "\"><script>alert('xss');</script>";

            // when
            String result = s3FileService.removeCdnUrls(inputHtml);

            // then
            assertThat(result)
                    .contains("img src=\"" + pureKey + "\"")
                    .doesNotContain("<script>"); // XSS 방어 살균 검증
        }
    }

    @Nested
    @DisplayName("미참조 파일(Orphan Files) 정리 테스트")
    class CleanUpOrphanFiles {

        @Test
        @DisplayName("S3 버킷 키 목록 중 DB 본문에 존재하지 않는 파일만 선별하여 삭제한다")
        void cleanUpOrphanFilesSuccessTest() {
            // given
            String usedKey = "12345678-1234-1234-1234-123456781234-used.png";
            String orphanKey = "abcdefab-abcd-abcd-abcd-abcdefabcdef-orphan.png";

            List<String> allPostContents = List.of("<img src=\"" + usedKey + "\">");
            List<String> s3BucketKeys = List.of(usedKey, orphanKey);

            given(s3FileHandler.getS3ObjectKeys()).willReturn(s3BucketKeys);

            // when
            s3FileService.cleanUpOrphanFiles(allPostContents);

            // then
            verify(s3FileHandler, times(1)).deleteFiles(List.of(orphanKey));
        }

        @Test
        @DisplayName("DB 데이터가 하나도 비어있을 경우 오작동 방지를 위해 삭제 처리를 중단한다")
        void cleanUpOrphanFilesEmptyPostContentsTest() {
            // given
            List<String> emptyContents = List.of();

            // when
            s3FileService.cleanUpOrphanFiles(emptyContents);

            // then
            verify(s3FileHandler, never()).getS3ObjectKeys();
            verify(s3FileHandler, never()).deleteFiles(any());
        }

        @Test
        @DisplayName("S3 오브젝트 키 목록의 크기를 정상적으로 반환한다")
        void s3ObjectCountSuccessTest() {
            // given
            List<String> mockKeys = List.of("images/file1.png", "images/file2.png", "images/file3.png");
            when(s3FileHandler.getS3ObjectKeys()).thenReturn(mockKeys);

            // when
            int count = s3FileService.s3ObjectCount();

            // then
            assertThat(count).isEqualTo(3);
            verify(s3FileHandler, times(1)).getS3ObjectKeys();
        }
    }
}