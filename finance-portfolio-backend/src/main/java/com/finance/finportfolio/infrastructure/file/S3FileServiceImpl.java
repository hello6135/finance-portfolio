package com.finance.finportfolio.infrastructure.file;

import java.io.IOException;
import java.io.InputStream;
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
            .addAttributes("img", "alt", "width", "height") // 이미지 관련 속성 허용
            .addTags("hr", "br"); // 가로줄, 줄바꿈 명시적 허용

    // 용량, 해상도 제한
    private static final long MAX_FILE_SIZE = 1L * 1024 * 1024;
    private static final int MAX_PIXEL_SIZE = 1920;

    // 1차 이미지 유효성 체크
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("파일 용량이 너무 큽니다.");
        }
    }

    @Override
    public int s3ObjectCount() {
        List<String> s3Keys = s3FileHandler.getS3ObjectKeys();
        return s3Keys.size();
    }

    @Override
    public String uploadFile(MultipartFile file) {

        validateFile(file);

        // 2차 이미지 유효성 체크
        try {
            // MIME 타입 검증
            String mimeType;
            try (InputStream inputStream = file.getInputStream()) {
                // S3Config의 tika 빈
                mimeType = tika.detect(inputStream);
            }
            if (!mimeType.startsWith("image/")) {
                throw new IllegalArgumentException("허용되지 않는 파일 형식입니다.");
            }

            // 3. 해상도 검증(메모리 부하 방지)
            try (InputStream inputStream = file.getInputStream()) {
                ImageInfo imageInfo = Imaging.getImageInfo(inputStream, file.getOriginalFilename());
                if (imageInfo.getWidth() > MAX_PIXEL_SIZE || imageInfo.getHeight() > MAX_PIXEL_SIZE) {
                    throw new IllegalArgumentException("이미지 해상도가 너무 높습니다.");
                }
            }

            String savedFileName = createFileName(file.getOriginalFilename());
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