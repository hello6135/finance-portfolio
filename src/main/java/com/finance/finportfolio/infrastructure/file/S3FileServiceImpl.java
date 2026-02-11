package com.finance.finportfolio.infrastructure.file;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Profile("dev")
public class S3FileServiceImpl implements FileService {

    private final S3FileHandler s3FileHandler;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            return null;
        }
        // 널체크랑 UUID로 이름만 정해주고 핸들러로
        String savedFileName = createFileName(file.getOriginalFilename());
        return s3FileHandler.uploadFile(file, savedFileName);

    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        s3FileHandler.deleteFile(fileUrl);
    }

    private String createFileName(String originalFileName) {
        return UUID.randomUUID() + "-" + originalFileName;
    }
}