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

    // @Override 원래 Url받아서 지우던 애
    // public void deleteFile(String fileUrl) {
    // if (fileUrl == null || fileUrl.isEmpty()) {
    // return;
    // }
    // String urlKey = extractKeyFromUrl(fileUrl);
    // if (urlKey != null) {
    // s3FileHandler.deleteFile(urlKey);
    // }
    // }

    // @Override 콘텐츠 전체 받아오던애
    // public void deleteFile(String content) {
    // if (content == null || content.isBlank())
    // return;

    // // 아까 만든 전문 메서드들 호출 (재사용!)
    // List<String> urls = extractUrlsFromHtml(content);

    // for (String url : urls) {
    // this.deleteFile(url); // 내부에서 extractKeyFromUrl 호출 후 S3 삭제
    // }
    // }
    @Override
    public void deleteFile(String content) {
        if (content == null || content.isBlank())
            return;

        // 정규식 메서드 재사용
        List<String> urls = extractFileNamesFromContent(content);

        for (String url : urls) {
            // (수정) this.deleteFile(url) 대신 전용 단일 삭제 메서드 호출
            deleteSingleFileByUrl(url);
        }
    }

    private void deleteSingleFileByUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty())
            return;
        String urlKey = extractKeyFromUrl(fileUrl);
        if (urlKey != null) {
            s3FileHandler.deleteFile(urlKey);
        }
    }

    @Override
    public List<String> cleanUpOrphanFiles(List<String> allPostContents) {
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

        return orphanKeys;
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