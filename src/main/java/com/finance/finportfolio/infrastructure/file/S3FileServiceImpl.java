package com.finance.finportfolio.infrastructure.file;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Profile("dev")
public class S3FileServiceImpl implements FileService {

    private final S3FileHandler s3FileHandler;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            return null;
        }
        // 널체크랑 UUID로 이름만 정해주고 핸들러로
        String savedFileName = createFileName(file.getOriginalFilename());
        return s3FileHandler.uploadFile(file, savedFileName);

    }

    private String createFileName(String originalFileName) {
        return UUID.randomUUID() + "-" + originalFileName;
    }

    @Override
    public void deleteFiles(String content) {
        if (content == null || content.isBlank())
            return;

        // 1. 본문에서 URL 추출 및 S3 Key로 변환
        List<String> keysToDelete = extractFileNamesFromContent(content).stream()
                .map(this::extractKeyFromUrl)
                .filter(Objects::nonNull)
                .toList();

        // 2. 추출된 키가 있다면 다중 삭제 로직 하나로 처리
        if (!keysToDelete.isEmpty()) {
            s3FileHandler.deleteFiles(keysToDelete);
        }
    }

    @Override
    public void cleanUpOrphanFiles(List<String> allPostContents) {
        Set<String> usedKeys = allPostContents.stream()
                // (수정) extractKeyFromUrl 대신 정규식 추출 메서드 사용
                .flatMap(content -> extractFileNamesFromContent(content).stream())
                .map(this::extractKeyFromUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<String> s3Keys = s3FileHandler.getS3ObjectKeys();

        List<String> orphanKeys = s3Keys.stream()
                .filter(key -> !usedKeys.contains(key))
                .toList();

        if (!orphanKeys.isEmpty()) {
            s3FileHandler.deleteFiles(orphanKeys);
        }
    }

    private String extractKeyFromUrl(String fileUrl) {
        try {
            // 이미 키(파일명)만 들어온 경우 처리
            if (!fileUrl.contains("/"))
                return fileUrl;

            URI uri = new URI(fileUrl);
            String path = uri.getPath();
            if (path == null || path.length() <= 1)
                return null;

            // 첫 슬래시(/) 제거 및 디코딩
            return URLDecoder.decode(path.substring(1), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("잘못된 URL 형식: {}", fileUrl);
            return null;
        }
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
}