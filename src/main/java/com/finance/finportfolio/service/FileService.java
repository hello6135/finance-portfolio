package com.finance.finportfolio.service;
//package com.example.portfolio.infrastructure.file;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    String store(MultipartFile file);
}