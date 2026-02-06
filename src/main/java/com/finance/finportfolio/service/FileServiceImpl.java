package com.finance.finportfolio.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.finance.finportfolio.infrastructure.FileHandler;

@Service
public class FileServiceImpl implements FileService {
    private final FileHandler fileHandler;

    public FileServiceImpl(FileHandler fileHandler) {
        this.fileHandler = fileHandler;
    }

    @Override
    public String store(MultipartFile file) {
        // 현재는 로컬/온프레미스 방식인 FileHandler를 사용
        return fileHandler.uploadFile(file);
    }
}
