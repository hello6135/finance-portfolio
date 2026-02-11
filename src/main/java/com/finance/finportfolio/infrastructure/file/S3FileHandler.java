package com.finance.finportfolio.infrastructure.file;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("dev")
public class S3FileHandler {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String uploadFile(MultipartFile file, String savedFileName) {

        // 2. S3 내 저장 경로 설정 (예: post/uuid_filename.jpg)
        String s3Key = savedFileName;

        // 3. 메타데이터 설정 (파일 타입과 크기)
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        try {
            // 4. S3에 파일 업로드 (ACL 설정 없이 업로드)
            amazonS3.putObject(new PutObjectRequest(bucket, s3Key, file.getInputStream(), metadata));
            // 5. 업로드된 파일의 공용 URL 반환
            String uploadUrl = amazonS3.getUrl(bucket, s3Key).toString();

            log.info("S3 파일 업로드 성공: {}, URL: {}", s3Key, uploadUrl);

            return uploadUrl;
        } catch (IOException e) {
            throw new RuntimeException("S3 파일 업로드 중 오류 발생", e);
        }
    }

    public void deleteFile(String fileUrl) {

        // URL에서 S3 Key(경로)만 추출 (예: post/uuid_filename.jpg)
        // URL 형식이 https://버킷명.s3.리전.amazonaws.com/경로 이므로 마지막 '/' 이후가 아니라 버킷명 이후 전체가
        // key입니다.

        try {
            // 2. URI 파싱 (JDK 21 권장 방식)
            URI uri = new URI(fileUrl);
            String path = uri.getPath();

            if (path == null || path.length() <= 1) {
                return;
            }

            // 3. S3 Key 추출 및 디코딩
            String key = URLDecoder.decode(path.substring(1), StandardCharsets.UTF_8);

            // 4. S3 삭제 요청
            amazonS3.deleteObject(bucket, key);
            log.info("S3 파일 삭제 성공: {}", key);

        } catch (URISyntaxException e) {
            log.error("잘못된 URL 형식입니다: {}", fileUrl);
        } catch (Exception e) {
            log.error("S3 파일 삭제 중 예기치 못한 오류 발생: {}", e.getMessage());
            // 필요에 따라 예외를 던지거나 로그만 남김
        }
    }
}
