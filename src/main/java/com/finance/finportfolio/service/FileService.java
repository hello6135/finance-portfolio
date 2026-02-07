package com.finance.finportfolio.service;
//package com.example.portfolio.infrastructure.file;

import org.springframework.web.multipart.MultipartFile;

// FileService - 파일 서비스 인터페이스
public interface FileService {
    String store(MultipartFile file);
}