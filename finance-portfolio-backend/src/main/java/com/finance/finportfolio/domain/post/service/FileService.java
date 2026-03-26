package com.finance.finportfolio.domain.post.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

// FileService - 파일 서비스 인터페이스
public interface FileService {
    String uploadFile(MultipartFile file);

    void deleteFiles(String content);

    void cleanUpOrphanFiles(List<String> allPostContents);

    String convertToCdnUrls(String content);

    String removeCdnUrls(String content);
}