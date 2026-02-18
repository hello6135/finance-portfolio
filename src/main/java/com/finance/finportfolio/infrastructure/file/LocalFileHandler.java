package com.finance.finportfolio.infrastructure.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component // 스프링 빈으로 등록하여 Service에서 가져다 쓸 수 있게 함
@Profile("local")
public class LocalFileHandler {

    @Value("${file.upload-dir:upload_images}")
    private String uploadDir;

    // 런타임 에러 : 필드 초기화 방식은 환경변수(@Value) 주입보다 선행 -> 메서드 방식 이용
    private String getFullPath() {
        return Paths.get(System.getProperty("user.dir"), uploadDir).toString();
    }

    // 물리적 이미지 파일 저장
    public void uploadFile(MultipartFile file, String savedFileName) {

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
    public void deleteFiles(List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty())
            return;

        String uploadPath = getFullPath(); // 파일 저장 기본 경로

        for (String fileName : fileNames) {
            try {
                // 파일 객체 생성
                File file = new File(uploadPath, fileName);

                if (file.exists()) {
                    if (file.delete()) {
                        log.info("[LOCAL] 파일 삭제 성공: {}", fileName);
                    } else {
                        log.warn("[LOCAL] 파일 삭제 실패 (권한 문제 등): {}", fileName);
                    }
                } else {
                    log.info("[LOCAL] 삭제할 파일이 존재하지 않습니다: {}", fileName);
                }
            } catch (SecurityException e) {
                log.error("[LOCAL] 파일 삭제 중 보안 예외 발생: {}", e.getMessage());
            } catch (Exception e) {
                log.error("[LOCAL] 알 수 없는 삭제 에러: {}", e.getMessage());
            }
        }
    }

    public List<String> getLocalFileNames() {
        try {
            Path uploadPath = Paths.get(getFullPath());

            // 디렉토리가 없으면 빈 리스트 반환 (방어 코드)
            if (!Files.exists(uploadPath)) {
                return Collections.emptyList();
            }

            // Files.list는 Stream을 반환하며, 사용 후 닫아주는게 좋으므로 try-with-resources 사용
            try (Stream<Path> stream = Files.list(uploadPath)) {
                return stream
                        .filter(Files::isRegularFile) // 디렉토리가 아닌 '파일'만 추출
                        .map(path -> path.getFileName().toString()) // 파일명만 추출 (uuid.png)
                        .toList();
            }
        } catch (IOException e) {
            log.error("로컬 파일 목록 조회 중 오류 발생: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}