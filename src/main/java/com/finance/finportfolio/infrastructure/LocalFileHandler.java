package com.finance.finportfolio.infrastructure;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component // 스프링 빈으로 등록하여 Service에서 가져다 쓸 수 있게 함
public class LocalFileHandler {

    @Value("${file.upload-dir:upload_images}")
    private String uploadDir;

    // 런타임 에러 : 필드 초기화 방식은 환경변수(@Value) 주입보다 선행 -> 메서드 방식 이용
    private String getFullPath() {
        return Paths.get(System.getProperty("user.dir"), uploadDir).toString();
    }

    // 물리적 이미지 파일 저장
    public void uploadFile(MultipartFile file, String savedFileName) {
        // 방어 코드: 파일이 아예 없거나(null) 비어있는 경우 처리
        if (file == null || file.isEmpty()) {
            return;
        }
        String uploadPath = getFullPath();

        log.info("게시글 이미지 파일 저장 시작! 대상 경로, 이름: {}, {}", uploadPath, savedFileName);

        // 저장할 폴더가 없으면 생성

        File targetFile = new File(uploadPath, savedFileName);
        File directory = targetFile.getParentFile();
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try {
            // 지정된 경로에 파일 물리적 저장
            file.transferTo(targetFile);
        } catch (IOException e) {
            throw new RuntimeException("게시글 이미지 파일 저장 중 에러가 발생했습니다.", e);
        }
    }

    // 물리적 이미지 파일 삭제
    public void deleteFile(String savedFileName) {
        if (savedFileName == null || savedFileName.isEmpty()) {
            return;
        }

        String uploadPath = getFullPath();
        File file = new File(uploadPath, savedFileName);

        if (file.exists()) {
            if (file.delete()) {
                log.info("게시글 이미지 파일 삭제 성공: {}", savedFileName);
            } else {
                log.warn("게시글 이미지 파일 삭제 실패: {}", savedFileName);
            }
        } else {
            log.info("게시글 이미지 파일이 존재하지 않습니다: {}", savedFileName);
        }
    }
}