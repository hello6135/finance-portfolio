package com.finance.finportfolio.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.finance.finportfolio.service.FileService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class EditorImageController {

    private final FileService fileService;

    @PostMapping("/api/image/upload")
    public Map<String, Object> upload(@RequestParam("upload") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 1. 파일을 저장하고 저장된 파일명을 받아옴
            String savedFileName = fileService.uploadFile(file);

            // 2. CKEditor 5 전용 성공 응답 규격
            response.put("uploaded", true);
            response.put("url", "/images/" + savedFileName); // WebConfig 리소스 매핑 주소

        } catch (Exception e) {
            // 3. 에러 발생 시 실패 응답 규격
            response.put("uploaded", false);

            Map<String, String> errorDetail = new HashMap<>();
            errorDetail.put("message", "이미지 업로드에 실패했습니다: " + e.getMessage());
            response.put("error", errorDetail);
        }

        return response; // 맵 객체를 최종 반환 (JSON으로 자동 변환됨)
    }
}