package com.finance.finportfolio.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.finance.finportfolio.infrastructure.FileHandler;

// FileServiceImpl - 파일 서비스 구현체 (docker 버전)
@Service
public class FileServiceImpl implements FileService {
    private final FileHandler fileHandler;

    public FileServiceImpl(FileHandler fileHandler) {
        this.fileHandler = fileHandler;
    }

    @Override
    public String store(MultipartFile file) {
        // 현재는 docker 방식인 FileHandler를 사용
        return fileHandler.uploadFile(file);
    }
}
