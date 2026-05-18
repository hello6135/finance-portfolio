package com.finance.finportfolio.global.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.finance.finportfolio.domain.post.service.FileService;
import com.finance.finportfolio.domain.post.service.PostService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3ImageCleanupScheduler {
    private final PostService postService;
    private final FileService fileService;

    // 매일 3시
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanUpOrphanImages() {
        log.info("S3 미참조 이미지 정기 정리 시작(0 0 3 * * *)");
        try {
            List<String> allPostContents = postService.getAllPostContents();

            if (allPostContents.isEmpty()) {
                log.info("S3 미참조 이미지 정기 정리 조기 종료(게시글 없음)");
                return;
            }

            fileService.cleanUpOrphanFiles(allPostContents);
            log.info("S3 미참조 이미지 정기 정리 종료(성공)");

        } catch (Exception e) {
            log.info("S3 미참조 이미지 정기 정리 조기 종료(예외 발생): ", e);
        }
    }
}
