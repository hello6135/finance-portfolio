package com.finance.finportfolio.infrastructure.file;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;

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

    // 다중 삭제
    public void deleteFiles(List<String> urlKeys) {
        if (urlKeys == null || urlKeys.isEmpty()) {
            return;
        }

        // DeleteObjectsRequest: S3에 요청을 하나씩 주고 받으면 네트워크 비효율이 심해 상자에 담아서 한번에 요청
        try {
            DeleteObjectsRequest request = new DeleteObjectsRequest(bucket)
                    .withKeys(urlKeys.toArray(new String[0]))
                    .withQuiet(false); // 성공 내역까지 보고, true: 에러 내역만 보고

            // quiet 모드를 false(기본값)로 두면 상세한 결과 수신 가능
            DeleteObjectsResult result = amazonS3.deleteObjects(request);
            log.info("S3 객체 삭제 완료: {} 건", result.getDeletedObjects().size());

        } catch (AmazonServiceException e) {
            // AWS 서버 측 에러 (권한 부족, 잘못된 버킷명 등)
            log.error("AWS S3 서비스 에러 발생: {}", e.getErrorMessage());
            // 서비스 로직에 따라 Custom Exception을 던지거나 로그만 남김
        } catch (SdkClientException e) {
            // 클라이언트 측 에러 (네트워크 연결 끊김 등)
            log.error("S3 연결 실패: {}", e.getMessage());
        } catch (Exception e) {
            log.error("S3 삭제 중 예상치 못한 에러: {}", e.getMessage());
        }
    }

    // S3버킷 모든 파일 키만 리스트로 반환
    public List<String> getS3ObjectKeys() {
        return amazonS3.listObjects(bucket).getObjectSummaries().stream()
                .map(S3ObjectSummary::getKey)
                .toList();
    }
}
