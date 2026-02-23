package com.finance.finportfolio.infrastructure.file;

import java.net.URI;
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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
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
        if (content == null || content.isBlank()) {
            return;
        }

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
    public String convertToCdnUrls(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        Document doc = Jsoup.parseBodyFragment(content);
        Elements imgs = doc.select("img[src]");

        for (Element img : imgs) {
            String src = img.attr("src");

            if (!src.startsWith("http") && !src.startsWith("/")) {
                String cdnUrl = "https://" + s3FileHandler.getCloudfrontDomain() + "/" + src;
                img.attr("src", cdnUrl);
            }
        }
        return doc.body().html();
    }

    // content에 섞여있는 img[src]들 CDN url에서 키 값으로 변경
    @Override
    public String removeCdnUrls(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        Document doc = Jsoup.parseBodyFragment(content);
        Elements imgs = doc.select("img[src]");

        for (Element img : imgs) {
            String src = img.attr("src");
            String pureKey = extractKeyFromUrl(src);
            if (pureKey != null) {
                img.attr("src", pureKey);
            }
        }
        log.info("살균된 content: {}", doc.body().html());

        return doc.body().html();
    }

    // http:, CDN url 등 경로 쳐내고 키 값 추출
    private String extractKeyFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }

        try {
            // 이미 키(파일명)만 들어온 경우 처리
            if (!fileUrl.contains("/")) {
                return fileUrl;
            }

            String key;
            if (fileUrl.startsWith("http")) {
                key = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            } else if (fileUrl.startsWith("/")) {
                key = fileUrl.substring(1);
            } else {
                key = fileUrl;
            }

            return URLDecoder.decode(key, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Url 추출 실패: {} | error: {}", fileUrl, e.getMessage());
            return null;
        }
    }

    // 미참조 파일 삭제
    @Override
    public void cleanUpOrphanFiles(List<String> allPostContents) {
        Set<String> usedKeys = allPostContents.stream()
                .flatMap(content -> extractFileNamesFromContent(content).stream())
                .collect(Collectors.toSet());

        List<String> s3Keys = s3FileHandler.getS3ObjectKeys();

        List<String> orphanKeys = s3Keys.stream()
                .filter(key -> !usedKeys.contains(key))
                .toList();

        if (!orphanKeys.isEmpty()) {
            log.info("미참조 파일 삭제 실행: {} 건", orphanKeys.size());
            s3FileHandler.deleteFiles(orphanKeys);
        }
    }

    // HTML 본문에서 URL/파일명 리스트를 추출하는 정규식 로직
    private List<String> extractFileNamesFromContent(String content) {
        List<String> imageKeys = new ArrayList<>();

        if (content == null || content.isBlank()) {
            return imageKeys;
        }

        // DB에는 도메인이 제거된 상태로 저장되므로, 순수 키 패턴(UUID 시작)만 찾아냄.
        // [a-f0-9\\-]{36} -> UUID 형태, 그 뒤에 파일명과 확장자
        String pureKeyPattern = "([a-f0-9\\-]{36}-[^\"'>\\s]+)";

        Pattern pattern = Pattern.compile(pureKeyPattern);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            // 정규식 그룹 1번 자체가 이미 순수 키값입니다.
            String key = matcher.group(1);
            imageKeys.add(URLDecoder.decode(key, StandardCharsets.UTF_8));
        }
        return imageKeys;
    }
}