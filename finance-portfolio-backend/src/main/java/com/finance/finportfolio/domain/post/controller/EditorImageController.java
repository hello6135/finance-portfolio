package com.finance.finportfolio.domain.post.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.finance.finportfolio.domain.post.service.FileService;
import com.finance.finportfolio.infrastructure.file.S3Properties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class EditorImageController {

    private final FileService fileService;
    private final S3Properties s3Properties;

    @PostMapping("/api/image/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("upload") MultipartFile file) {

        try {
            // 파일을 저장하고 저장된 파일명을 받아옴
            String savedFileName = fileService.uploadFile(file);

            // cdn url로
            String cdnUrl = String.format("https://%s/%s",
                    s3Properties.cloudfrontDomain(),
                    savedFileName);

            return ResponseEntity.ok(Map.of(
                    "uploaded", true,
                    "url", cdnUrl));
        } catch (Exception e) {
            log.error("CKEditor image upload failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "uploaded", false,
                    "error", Map.of("message", e.getMessage()) // 백엔드 메시지 그대로 전달
            ));
        }
    }
}