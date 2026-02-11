package com.finance.finportfolio.infrastructure.file;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

// LocalFileServiceImpl - 파일 서비스 구현체 (docker 버전)
@Service
@RequiredArgsConstructor
@Profile("local")
public class LocalFileServiceImpl implements FileService {
    private final LocalFileHandler localFileHandler;

    @Override
    public String uploadFile(MultipartFile file) {

        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            return null;
        }
        String originalFileName = file.getOriginalFilename();
        String uuid = UUID.randomUUID().toString(); // 중복 방지

        // 확장자 추출 (예: .jpg, .png)
        String extension = "";
        int dotIndex = originalFileName.lastIndexOf(".");
        if (dotIndex > 0) {
            extension = originalFileName.substring(dotIndex);
        }
        // 저장 요청 시간 추가
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd_HHmmss");
        String todayTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")).format(formatter);
        // 서버에 저장될 파일 이름
        String savedFileName = todayTime + "_" + uuid + extension;

        localFileHandler.uploadFile(file, savedFileName);

        return savedFileName;
    }

    @Override
    public void deleteFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return;
        }

        localFileHandler.deleteFile(fileName);
    }
}
