package com.finance.finportfolio.service;
//package com.example.portfolio.infrastructure.file;

import org.springframework.web.multipart.MultipartFile;

// FileService - 파일 서비스 인터페이스
public interface FileService {
    String uploadFile(MultipartFile file);

    void deleteFile(String fileName);
}