package com.finance.finportfolio.infrastructure;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component // 스프링 빈으로 등록하여 Service에서 가져다 쓸 수 있게 함
public class FileHandler {

    @Value("${file.upload-dir:upload_images}")
    private String uploadDir;

    private String getFullPath() {
        return Paths.get(System.getProperty("user.dir"), uploadDir).toString();
    }
    // 필드 초기화가 @Value 주입보다 선행해서 uploadPath에 null값이 들어가서 수정
    // private final String uploadPath =
    // Paths.get(System.getProperty("user.dir"),uploadDir).toString();

    public String uploadFile(MultipartFile file) {
        // 💡 추가된 방어 코드: 파일이 아예 없거나(null) 비어있는 경우 처리
        if (file == null || file.isEmpty()) {
            return null;
        }
        String uploadPath = getFullPath(); // 메서드를 통해 이미지 경로를 가져옴

        log.info("파일 저장 시작! 대상 경로: {}", uploadPath);

        String originalFileName = file.getOriginalFilename();
        String uuid = UUID.randomUUID().toString();

        // 파일명이 null일 경우를 대비한 추가 방어
        if (originalFileName == null) {
            return null;
        }

        // 2. 확장자 추출 (예: .jpg, .png)
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));

        // 3. 서버에 저장될 실제 파일 이름
        String savedFileName = uuid + extension;

        // 4. 저장할 폴더가 없으면 생성
        File directory = new File(uploadPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try {
            // 5. 지정된 경로에 파일 물리적 저장
            File targetFile = new File(uploadPath, savedFileName);
            file.transferTo(targetFile);

            return savedFileName; // DB에는 저장된 파일명(또는 경로)을 리턴

        } catch (IOException e) {
            throw new RuntimeException("파일 저장 중 에러가 발생했습니다.", e);
        }
    }

    public void deleteFile(String savedFileName) {
        // 2. 물리적 이미지 파일 삭제 로직
        if (savedFileName == null || savedFileName.isEmpty()) {
            return;
        }

        String uploadPath = getFullPath(); // 메서드를 통해 안전하게 경로를 가져옴
        // WebConfig와 FileHandler에서 설정한 폴더명과 일치해야 함
        File file = new File(uploadPath, savedFileName);

        if (file.exists()) {
            if (file.delete()) {
                log.info("파일 삭제 성공: {}", savedFileName);
            } else {
                log.warn("파일 삭제 실패: {}", savedFileName);
            }
        } else {
            log.info("파일이 존재하지 않습니다: {}", savedFileName);
        }
    }
}