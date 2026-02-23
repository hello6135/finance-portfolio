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
        log.info("작동확인1: {}", content);

        // 1. HTML 파싱 (body 태그 내부 내용만 다룸)
        Document doc = Jsoup.parseBodyFragment(content);

        // 2. img 태그 중 src 속성이 있는 요소들을 선택
        Elements imgs = doc.select("img[src]");

        for (Element img : imgs) {
            String src = img.attr("src");

            // 3. 조건 검사: 이미 전체 경로(http)가 아니거나 로컬 경로가 아닌 경우 (순수 키값인 경우)
            // DB에 uuid-name.png 형태로 저장되어 있다고 가정합니다.
            if (!src.startsWith("http") && !src.startsWith("/")) {
                // CloudFront 도메인을 붙여서 새로운 URL 생성
                // s3Properties.getCloudfrontDomain() 부분은 실제 환경 변수나 설정값을 담은 필드로 대체하세요.
                String cdnUrl = "https://" + s3FileHandler.getCloudfrontDomain() + "/" + src;
                img.attr("src", cdnUrl);
            }
        }
        log.info("작동확인2: {}", doc.body().html());

        // 4. 변환된 HTML의 body 내용물만 반환
        return doc.body().html();
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
        List<String> imageKeys = new ArrayList<>();
        if (content == null || content.isBlank())
            return imageKeys;

        String domain = s3FileHandler.getCloudfrontDomain();

        // 1. CDN URL 형태도 찾고 (이미 변환된 경우 대비)
        String cdnPattern = "(?:https?://" + Pattern.quote(domain) + "/)([^\"'>\\s]+)";
        // 2. 순수 파일명 형태도 찾음 (DB 저장 형태) - UUID 특성상 하이픈(-)과 확장자 체크
        String pureKeyPattern = "([a-f0-9\\-]{36}-[^\"'>\\s]+)";

        // 두 패턴을 모두 합쳐서 찾기
        String combinedRegex = cdnPattern + "|" + pureKeyPattern;
        Pattern pattern = Pattern.compile(combinedRegex);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            // matcher.group(1)은 CDN 패턴에서 찾은 키, matcher.group(2)는 순수 키 패턴
            String key = (matcher.group(1) != null) ? matcher.group(1) : matcher.group(2);
            if (key != null) {
                imageKeys.add(URLDecoder.decode(key, StandardCharsets.UTF_8));
            }
        }
        return imageKeys;
    }
}