package com.finance.finportfolio.infrastructure.file;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

// S3FileServiceImpl - 파일 서비스 구현체 (AWS S3 버전)
@Service
@RequiredArgsConstructor
@Profile("prod")
public class S3FileServiceImpl implements FileService {

    @Override
    public String uploadFile(MultipartFile file) {
        return "";
    }

    @Override
    public void deleteFile(String fileName) {
    }
}
