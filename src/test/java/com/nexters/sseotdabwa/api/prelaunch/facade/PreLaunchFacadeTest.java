package com.nexters.sseotdabwa.api.prelaunch.facade;

import com.nexters.sseotdabwa.api.prelaunch.dto.PreLaunchEmailRequest;
import com.nexters.sseotdabwa.api.prelaunch.dto.PreLaunchEmailResponse;
import com.nexters.sseotdabwa.common.exception.GlobalException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PreLaunchFacadeTest {

    @Autowired
    private PreLaunchFacade preLaunchFacade;

    @Test
    @DisplayName("이메일 등록 성공")
    void registerEmail_success() {
        // given
        PreLaunchEmailRequest request = new PreLaunchEmailRequest("test@example.com");

        // when
        PreLaunchEmailResponse response = preLaunchFacade.registerEmail(request);

        // then
        assertThat(response.id()).isNotNull();
        assertThat(response.email()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("중복 이메일 등록 시 PRELAUNCH_001 에러")
    void registerEmail_duplicate_throwsPrelaunch001() {
        // given
        PreLaunchEmailRequest request = new PreLaunchEmailRequest("duplicate@example.com");
        preLaunchFacade.registerEmail(request);

        // when & then
        assertThatThrownBy(() -> preLaunchFacade.registerEmail(request))
                .isInstanceOf(GlobalException.class)
                .hasMessage("이미 등록된 이메일입니다.");
    }
}
