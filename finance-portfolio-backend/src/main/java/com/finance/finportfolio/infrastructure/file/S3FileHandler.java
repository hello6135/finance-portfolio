package com.finance.finportfolio.infrastructure.file;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.finance.finportfolio.global.error.exception.FileStorageException;

import io.awspring.cloud.s3.S3Template;
import io.awspring.cloud.s3.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.S3Exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("local | dev | prod")
public class S3FileHandler {

    private final S3Template s3Template;
    private final S3Properties s3Properties;

    public String uploadFile(MultipartFile file, String savedFileName, String mimeType) {
        try {
            // 1. 메타데이터 객체 생성 및 설정 (빌더 패턴 활용)
            ObjectMetadata metadata = ObjectMetadata.builder()
                    .contentType(mimeType)
                    .contentLength(file.getSize())
                    .build();

            // 2. 업로드 수행
            s3Template.upload(
                    s3Properties.bucketName(),
                    savedFileName,
                    file.getInputStream(),
                    metadata // 람다 대신 객체를 직접 전달
            );

            log.info("S3 파일 업로드 성공: {}", savedFileName);

            return savedFileName;

        } catch (IOException e) {
            log.error("S3 파일 읽기 에러: {}", e.getMessage());
            throw new FileStorageException("S3 파일 업로드 중 오류 발생", e);
        } catch (S3Exception e) {
            log.error("AWS S3 서비스 에러: {}", e.awsErrorDetails().errorMessage());
            throw new FileStorageException("AWS S3 서비스 오류", e);
        }
    }

    // 다중 삭제
    public void deleteFiles(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        try {
            // S3Template은 리스트를 받아 반복 삭제.(S3Client Batch 방식으로 다중 삭제 refactor 가능!)
            keys.forEach(key -> {
                try {
                    s3Template.deleteObject(s3Properties.bucketName(), key);
                    log.debug("S3 객체 삭제 시도: {}", key);
                } catch (Exception e) {
                    log.error("S3 개별 객체 삭제 실패 [key: {}]: {}", key, e.getMessage());
                }
            });

            log.info("S3 객체 삭제 완료: {} 건", keys.size());
        } catch (S3Exception e) {
            log.error("S3 삭제 중 중대한 에러 발생: {}", e.awsErrorDetails().errorMessage());
        }
    }

    public String getCloudfrontDomain() {
        return s3Properties.cloudfrontDomain();
    }

    // S3버킷 모든 파일 키만 리스트로 반환
    public List<String> getS3ObjectKeys() {
        return s3Template.listObjects(s3Properties.bucketName(), "").stream()
                .map(resource -> resource.getLocation().getObject())
                .filter(Objects::nonNull)
                .filter(filename -> !filename.isEmpty())
                .toList();
    }
}
