package com.finance.finportfolio.global.scheduler;

import com.finance.finportfolio.domain.post.service.FileService;
import com.finance.finportfolio.domain.post.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ImageCleanupSchedulerTest {

    @Mock
    private PostService postService;

    @Mock
    private FileService fileService;

    @InjectMocks
    private S3ImageCleanupScheduler scheduler;

    @Test
    @DisplayName("게시글이 존재하면 cleanUpOrphanFiles를 호출한다")
    void cleanUpOrphanImages_withPosts_callsCleanUp() {
        // given
        List<String> contents = List.of("<img src='https://s3.../image1.png'>", "내용2");
        when(postService.getAllPostContents()).thenReturn(contents);

        // when
        scheduler.cleanUpOrphanImages();

        // then
        verify(postService, times(1)).getAllPostContents();
        verify(fileService, times(1)).cleanUpOrphanFiles(contents);
    }

    @Test
    @DisplayName("게시글이 없으면 cleanUpOrphanFiles를 호출하지 않는다")
    void cleanUpOrphanImages_noPosts_skipsCleanUp() {
        // given
        when(postService.getAllPostContents()).thenReturn(Collections.emptyList());

        // when
        scheduler.cleanUpOrphanImages();

        // then
        verify(postService, times(1)).getAllPostContents();
        verify(fileService, never()).cleanUpOrphanFiles(any());
    }

    @Test
    @DisplayName("PostService에서 예외 발생 시 cleanUpOrphanFiles를 호출하지 않는다")
    void cleanUpOrphanImages_postServiceThrows_doesNotPropagateException() {
        // given
        when(postService.getAllPostContents()).thenThrow(new RuntimeException("DB 오류"));

        // when & then (예외가 외부로 전파되지 않아야 함)
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> scheduler.cleanUpOrphanImages());
        verify(fileService, never()).cleanUpOrphanFiles(any());
    }

    @Test
    @DisplayName("FileService에서 예외 발생 시 예외가 외부로 전파되지 않는다")
    void cleanUpOrphanImages_fileServiceThrows_doesNotPropagateException() {
        // given
        List<String> contents = List.of("내용1");
        when(postService.getAllPostContents()).thenReturn(contents);
        doThrow(new RuntimeException("S3 오류")).when(fileService).cleanUpOrphanFiles(any());

        // when & then
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> scheduler.cleanUpOrphanImages());
    }
}