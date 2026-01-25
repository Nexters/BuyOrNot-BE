package com.nexters.sseotdabwa.domain.users.service;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.api.users.exception.UserErrorCode;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;
import com.nexters.sseotdabwa.domain.users.service.command.UserCreateCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("소셜 ID와 Provider로 사용자 조회 성공")
    void findBySocialIdAndProvider_success() {
        // given
        User user = User.builder()
                .socialId("12345")
                .email("test@kakao.com")
                .nickname("테스트")
                .socialAccount(SocialAccount.KAKAO)
                .build();
        userRepository.save(user);

        // when
        Optional<User> found = userService.findBySocialIdAndProvider("12345", SocialAccount.KAKAO);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getSocialId()).isEqualTo("12345");
    }

    @Test
    @DisplayName("존재하지 않는 소셜 ID로 조회 시 빈 Optional 반환")
    void findBySocialIdAndProvider_notFound_returnsEmpty() {
        // when
        Optional<User> found = userService.findBySocialIdAndProvider("nonexistent", SocialAccount.KAKAO);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("ID로 사용자 조회 성공")
    void findById_success() {
        // given
        User user = User.builder()
                .socialId("12345")
                .email("test@kakao.com")
                .nickname("테스트")
                .socialAccount(SocialAccount.KAKAO)
                .build();
        User savedUser = userRepository.save(user);

        // when
        User found = userService.findById(savedUser.getId());

        // then
        assertThat(found.getId()).isEqualTo(savedUser.getId());
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 예외 발생")
    void findById_notFound_throwsException() {
        // given
        Long nonExistentId = 99999L;

        // when & then
        assertThatThrownBy(() -> userService.findById(nonExistentId))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자 생성 성공")
    void createUser_success() {
        // given
        UserCreateCommand command = new UserCreateCommand(
                "12345",
                "test@kakao.com",
                "테스트",
                SocialAccount.KAKAO,
                "https://example.com/profile.jpg"
        );

        // when
        User created = userService.createUser(command);

        // then
        assertThat(created.getId()).isNotNull();
        assertThat(created.getSocialId()).isEqualTo("12345");
        assertThat(created.getEmail()).isEqualTo("test@kakao.com");
        assertThat(created.getNickname()).isEqualTo("테스트");
        assertThat(created.getSocialAccount()).isEqualTo(SocialAccount.KAKAO);
    }

    @Test
    @DisplayName("프로필 업데이트 성공")
    void updateProfile_success() {
        // given
        User user = User.builder()
                .socialId("12345")
                .email("test@kakao.com")
                .nickname("기존 닉네임")
                .socialAccount(SocialAccount.KAKAO)
                .profileImage("https://example.com/old.jpg")
                .build();
        userRepository.save(user);

        // when
        userService.updateProfile(user, "새 닉네임", "https://example.com/new.jpg");

        // then
        assertThat(user.getNickname()).isEqualTo("새 닉네임");
        assertThat(user.getProfileImage()).isEqualTo("https://example.com/new.jpg");
    }
}
