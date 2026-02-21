package com.nexters.sseotdabwa.domain.prelaunch.service;

import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.prelaunch.entity.PreLaunchEmail;
import com.nexters.sseotdabwa.domain.prelaunch.repository.PreLaunchEmailRepository;
import com.nexters.sseotdabwa.domain.prelaunch.service.command.PreLaunchEmailCreateCommand;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PreLaunchEmailServiceTest {

    @Autowired
    private PreLaunchEmailService preLaunchEmailService;

    @Autowired
    private PreLaunchEmailRepository preLaunchEmailRepository;

    @Test
    @DisplayName("이메일 등록 성공")
    void createEmail_success() {
        // given
        PreLaunchEmailCreateCommand command = new PreLaunchEmailCreateCommand("test@example.com");

        // when
        PreLaunchEmail result = preLaunchEmailService.createEmail(command);

        // then
        assertThat(result.getId()).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("중복 이메일 등록 시 PRELAUNCH_001 에러")
    void createEmail_duplicate_throwsPrelaunch001() {
        // given
        PreLaunchEmailCreateCommand command = new PreLaunchEmailCreateCommand("duplicate@example.com");
        preLaunchEmailService.createEmail(command);

        // when & then
        assertThatThrownBy(() -> preLaunchEmailService.createEmail(command))
                .isInstanceOf(GlobalException.class)
                .hasMessage("이미 등록된 이메일입니다.");
    }
}
