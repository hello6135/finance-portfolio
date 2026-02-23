package com.finance.finportfolio.infrastructure.file;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import io.awspring.cloud.s3.ObjectMetadata;
import software.amazon.awssdk.services.s3.model.S3Exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("dev")
public class S3FileHandler {

    private final S3Template s3Template;
    private final S3Properties s3Properties;

    public String uploadFile(MultipartFile file, String savedFileName) {
        try {
            // 1. 메타데이터 객체 생성 및 설정 (빌더 패턴 활용)
            ObjectMetadata metadata = ObjectMetadata.builder()
                    .contentType(file.getContentType())
                    .build();

            // 2. 업로드 수행
            S3Resource resource = s3Template.upload(
                    s3Properties.bucketName(),
                    savedFileName,
                    file.getInputStream(),
                    metadata // 람다 대신 객체를 직접 전달
            );
            log.info("S3 파일 업로드 성공: {}", savedFileName);
            return savedFileName;

        } catch (IOException e) {
            log.error("S3 파일 읽기 에러: {}", e.getMessage());
            throw new RuntimeException("S3 파일 업로드 중 오류 발생", e);
        } catch (S3Exception e) {
            log.error("AWS S3 서비스 에러: {}", e.awsErrorDetails().errorMessage());
            throw new RuntimeException("AWS S3 서비스 오류", e);
        }
    }

    // 다중 삭제
    public void deleteFiles(List<String> urlKeys) {
        if (urlKeys == null || urlKeys.isEmpty()) {
            return;
        }

        try {
            // S3Template은 리스트를 받아 일괄 삭제(Batch Delete)를 효율적으로 수행합니다.
            urlKeys.forEach(key -> s3Template.deleteObject(s3Properties.bucketName(), key));
            log.info("S3 객체 삭제 완료: {} 건", urlKeys.size());

        } catch (S3Exception e) {
            log.error("S3 삭제 중 에러 발생: {}", e.awsErrorDetails().errorMessage());
        }
    }

    public String getCloudfrontDomain() {
        return s3Properties.cloudfrontDomain();
    }

    // S3버킷 모든 파일 키만 리스트로 반환
    public List<String> getS3ObjectKeys() {
        // listObjects가 훨씬 간결해졌으며, 스트림 처리에 최적화되어 있습니다.
        return s3Template.listObjects(s3Properties.bucketName(), "")
                .stream()
                // S3Resource::getFilename은 String을 반환하므로 타입 추론이 명확해집니다.
                .map(resource -> {
                    return resource.getLocation().getObject();
                })
                .filter(filename -> filename != null && !filename.isEmpty())
                .toList();
    }
}
