package com.finance.finportfolio.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.*;

class SchedulerConfigTest {

    private final SchedulerConfig schedulerConfig = new SchedulerConfig();

    @Test
    @DisplayName("TaskScheduler가 ScheduledTaskRegistrar에 등록된다")
    void configureTasks_registersTaskScheduler() {
        // given
        ScheduledTaskRegistrar taskRegistrar = mock(ScheduledTaskRegistrar.class);

        // when
        schedulerConfig.configureTasks(taskRegistrar);

        // then
        var captor = forClass(ThreadPoolTaskScheduler.class);
        verify(taskRegistrar, times(1)).setTaskScheduler(captor.capture());

        ThreadPoolTaskScheduler scheduler = captor.getValue();
        // getPoolSize() → 실제 풀 크기 (initialize() 후 executor 기준)
        // getCorePoolSize() → initialize() 이후 실제 설정된 코어 스레드 수
        assertThat(scheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(1);
        assertThat(scheduler.getThreadNamePrefix()).isEqualTo("s3-cleanup-cron-");
    }

    @Test
    @DisplayName("configureTasks 호출 시 예외가 발생하지 않는다")
    void configureTasks_doesNotThrow() {
        // given
        ScheduledTaskRegistrar taskRegistrar = new ScheduledTaskRegistrar();

        // when & then
        assertThatCode(() -> schedulerConfig.configureTasks(taskRegistrar))
                .doesNotThrowAnyException();
    }
}