package com.finance.finportfolio.infrastructure.file;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// LocalFileServiceImpl - 파일 서비스 구현체 (docker 버전)
@Slf4j
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

        return "/images/" + savedFileName;
    }

    @Override
    public void deleteFiles(String content) {

        if (content == null || content.isBlank())
            return;

        // 1. 본문에서 URL 추출 및 S3 Key로 변환
        List<String> fileNameToDelete = extractFileNamesFromContent(content);

        // 2. 추출된 키가 있다면 다중 삭제 로직 하나로 처리
        if (!fileNameToDelete.isEmpty()) {
            localFileHandler.deleteFiles(fileNameToDelete);
        }
    }

    @Override
    public String convertToCdnUrls(String content) {
        return content;
    }

    // HTML 본문에서 URL/파일명 리스트를 추출하는 정규식 로직
    private List<String> extractFileNamesFromContent(String content) {
        List<String> fileNames = new ArrayList<>();
        if (content == null || content.isBlank())
            return fileNames;

        Pattern pattern = Pattern.compile(
                "/images/([^\"'>\\s]+)|(https://[a-zA-Z0-9.-]+\\.s3\\.[a-zA-Z0-9-]+\\.amazonaws\\.com/[^\"'>\\s]+)");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String localFileName = matcher.group(1);
            String s3FullUrl = matcher.group(2);

            if (localFileName != null)
                fileNames.add(localFileName);
            else if (s3FullUrl != null)
                fileNames.add(s3FullUrl);
        }
        return fileNames;
    }

    @Override
    public void cleanUpOrphanFiles(List<String> allPostContents) {
        Set<String> usedKeys = allPostContents.stream()
                // (수정) extractKeyFromUrl 대신 정규식 추출 메서드 사용
                .flatMap(content -> extractFileNamesFromContent(content).stream())
                .map(path -> {
                    if (path.startsWith("http")) {
                        return path.substring(path.lastIndexOf("/") + 1);
                    }
                    return path; // 로컬은 이미 파일명임
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<String> s3Keys = localFileHandler.getLocalFileNames();

        List<String> orphanKeys = s3Keys.stream()
                .filter(key -> !usedKeys.contains(key))
                .toList();

        if (!orphanKeys.isEmpty()) {
            localFileHandler.deleteFiles(orphanKeys);
        }
    }
}
