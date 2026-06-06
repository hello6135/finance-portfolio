package com.finance.finportfolio.infrastructure.file;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.Imaging;
import org.apache.tika.Tika;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.finance.finportfolio.domain.post.service.FileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Profile("local | dev | prod")
public class S3FileServiceImpl implements FileService {

    private final S3FileHandler s3FileHandler;
    private final Tika tika;

    // jsoup 커스텀 설정 본문용(utext)
    private static final Safelist HTML_SAFE_LIST = Safelist.relaxed()
            // CKEditor 필수 기본 태그 및 가로줄/줄바꿈 추가 허용
            .addTags("hr", "br", "span", "div")
            // 이미지 태그의 필수 속성 확장
            .addAttributes("img", "alt", "width", "height", "src")
            // 링크(a) 태그의 target 속성 허용 (새창 열기용)
            .addAttributes("a", "target", "rel")
            .addProtocols("a", "href", "http", "https", "mailto")
            // 모든 허용된 태그에 대해 인라인 스타일(style) 속성 허용
            // (텍스트 정렬, 글자 색상, 배경색 유지)
            .addAttributes(":all", "style")
            // 테이블(표) 서식 보존을 위한 속성 추가 허용
            .addAttributes("table", "border", "cellspacing", "cellpadding")
            .addAttributes("td", "colspan", "rowspan")
            .addAttributes("th", "colspan", "rowspan");

    // 용량, 해상도 제한
    private static final long MAX_FILE_SIZE = 1L * 1024 * 1024;
    private static final int MAX_PIXEL_SIZE = 1920;

    // 허용되는 MIME 타입 및 확장자 리스트 (교차 검증 및 화이트리스트용)
    private static final Map<String, List<String>> ALLOWED_MIME_EXTENSIONS = Map.of(
            "image/png", List.of("png"),
            "image/jpeg", List.of("jpg", "jpeg"),
            "image/gif", List.of("gif"),
            "image/webp", List.of("webp"),
            "image/bmp", List.of("bmp")
    );

    // 1차 이미지 유효성 체크
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 용량이 너무 큽니다.");
        }
    }

    // 파일명 확장자와 Tika가 감지한 MIME 타입 간의 교차 검증
    private void validateImageMimeAndExtension(String originalFilename, String mimeType) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("올바르지 않은 파일명입니다.");
        }

        if (!ALLOWED_MIME_EXTENSIONS.containsKey(mimeType)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다.");
        }

        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == originalFilename.length() - 1) {
            throw new IllegalArgumentException("파일 확장자가 올바르지 않습니다.");
        }

        String extension = originalFilename.substring(lastDotIndex + 1).toLowerCase();
        List<String> allowedExtensions = ALLOWED_MIME_EXTENSIONS.get(mimeType);

        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("실제 파일 형식과 확장자가 일치하지 않습니다.");
        }
    }

    @Override
    public int s3ObjectCount() {
        List<String> s3Keys = s3FileHandler.getS3ObjectKeys();
        return s3Keys.size();
    }

    @Override
    public String uploadFile(MultipartFile file) {

        // 1차 이미지 유효성 체크
        validateFile(file);

        // 2차 이미지 유효성 체크
        try {
            // MIME 타입 검증
            String mimeType;
            try (InputStream inputStream = file.getInputStream()) {
                // S3Config의 tika 빈
                mimeType = tika.detect(inputStream);
            }

            // 실제 파일 바이너리(Tika)와 확장자 교차 검증
            String originalFilename = file.getOriginalFilename();
            validateImageMimeAndExtension(originalFilename, mimeType);

            // 해상도 검증(메모리 부하 방지 및 파일명 의존성 제거)
            // 사용자 지정 파일명 대신, 안전한 Tika 검증 기반 더미 파일명을 사용하여 Imaging 분석
            String dummyFilename = "dummy." + ALLOWED_MIME_EXTENSIONS.get(mimeType).get(0);
            try (InputStream inputStream = file.getInputStream()) {
                ImageInfo imageInfo = Imaging.getImageInfo(inputStream, dummyFilename);
                if (imageInfo.getWidth() > MAX_PIXEL_SIZE || imageInfo.getHeight() > MAX_PIXEL_SIZE) {
                    throw new IllegalArgumentException("이미지 해상도가 너무 높습니다.");
                }
            }

            String savedFileName = createFileName(originalFilename);
            return s3FileHandler.uploadFile(file, savedFileName, mimeType);

        } catch (IOException e) {
            log.error("이미지 분석 또는 파일 읽기 실패: {}", e.getMessage());
            throw new IllegalArgumentException("올바르지 않은 이미지 형식 및 읽기 오류입니다.");
        }
    }

    private String createFileName(String originalFileName) {
        String safeName = originalFileName.replaceAll("\\s", "_"); // 공백 제거
        return UUID.randomUUID() + "-" + safeName;
    }

    @Override
    public void deleteFiles(String content) {
        if (content == null || content.isBlank()) {
            return;
        }

        // 본문에서 URL 추출 및 S3 Key로 변환
        List<String> keysToDelete = extractFileNamesFromContent(content).stream()
                .map(this::extractKeyFromUrl)
                .filter(Objects::nonNull)
                .toList();

        // 추출된 키가 있다면 다중 삭제 로직 하나로 처리
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

        String cleanedHtml = Jsoup.clean(content, HTML_SAFE_LIST);

        Document doc = Jsoup.parseBodyFragment(cleanedHtml);
        Elements imgs = doc.select("img[src]");

        for (Element img : imgs) {
            String src = img.attr("src");
            String pureKey = extractKeyFromUrl(src);
            if (pureKey != null) {
                img.attr("src", pureKey);
            }
        }

        String finalContent = doc.body().html();
        log.info("살균된 content: {}", finalContent);

        return finalContent;
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
        if (allPostContents.isEmpty()) {
            log.warn("DB 데이터가 비어있어 삭제 로직을 중단합니다.");
            return;
        }

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
        String pureKeyPattern = "([a-f0-9\\-]{36}-[^\"'>]+)";

        Pattern pattern = Pattern.compile(pureKeyPattern);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            // 정규식 그룹 1번 자체가 이미 순수 키값입니다.
            String key = matcher.group(1);
            imageKeys.add(URLDecoder.decode(key.trim(), StandardCharsets.UTF_8));
        }
        return imageKeys;
    }
}