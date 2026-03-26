package com.finance.finportfolio.domain.post.controller;

import java.util.HashMap;
import java.util.Map;

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
    public Map<String, Object> upload(@RequestParam("upload") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 1. 파일을 저장하고 저장된 파일명을 받아옴
            String savedFileName = fileService.uploadFile(file);

            String cdnUrl = String.format("https://%s/%s",
                    s3Properties.cloudfrontDomain(),
                    savedFileName);

            // 2. CKEditor 5 전용 성공 응답 규격
            response.put("uploaded", true);
            response.put("url", cdnUrl);

            log.info("CKEditor image uploaded successfully: {}", savedFileName);
        } catch (Exception e) {
            log.error("CKEditor image upload failed", e);

            response.put("uploaded", false);
            response.put("error", Map.of("message", "이미지 업로드 실패: " + e.getMessage()));
        }

        return response; // 맵 객체를 최종 반환 (JSON으로 자동 변환됨)
    }
}