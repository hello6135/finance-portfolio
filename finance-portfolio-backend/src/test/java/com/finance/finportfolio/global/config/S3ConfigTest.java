package com.finance.finportfolio.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.apache.tika.Tika;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = S3Config.class)
@ActiveProfiles("local")
class S3ConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Tika tika;

    @Test
    @DisplayName("S3Config를 통해 Tika 빈이 스프링 컨테이너에 정상 등록된다")
    void tikaBeanRegistrationTest() {
        // when
        Tika registeredBean = applicationContext.getBean(Tika.class);

        // then
        assertThat(registeredBean).isNotNull();
        assertThat(registeredBean).isSameAs(tika);
    }

    @Test
    @DisplayName("Tika 빈이 파일의 MIME 타입을 올바르게 식별한다")
    void tikaMimeTypeDetectionTest() throws IOException {
        // given
        MockMultipartFile mockPngFile = new MockMultipartFile(
                "file",
                "test-image.png",
                "image/png",
                new byte[] { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' } // PNG Magic Number
        );

        // when
        String detectedType = tika.detect(mockPngFile.getBytes());

        // then
        assertThat(detectedType).isEqualTo("image/png");
    }
}