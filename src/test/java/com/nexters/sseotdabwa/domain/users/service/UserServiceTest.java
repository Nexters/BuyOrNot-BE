package com.nexters.sseotdabwa.domain.users.service;

import java.util.Optional;
import java.util.UUID;

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
        String uniqueSocialId = UUID.randomUUID().toString();
        String uniqueNickname = "테스트_" + UUID.randomUUID().toString().substring(0, 8);
        User user = User.builder()
                .socialId(uniqueSocialId)
                .nickname(uniqueNickname)
                .socialAccount(SocialAccount.KAKAO)
                .build();
        userRepository.save(user);

        // when
        Optional<User> found = userService.findBySocialIdAndProvider(uniqueSocialId, SocialAccount.KAKAO);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getSocialId()).isEqualTo(uniqueSocialId);
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
        String uniqueSocialId = UUID.randomUUID().toString();
        String uniqueNickname = "테스트_" + UUID.randomUUID().toString().substring(0, 8);
        User user = User.builder()
                .socialId(uniqueSocialId)
                .nickname(uniqueNickname)
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
        String uniqueSocialId = UUID.randomUUID().toString();
        String uniqueNickname = "테스트_" + UUID.randomUUID().toString().substring(0, 8);
        UserCreateCommand command = new UserCreateCommand(
                uniqueSocialId,
                uniqueNickname,
                SocialAccount.KAKAO,
                "https://example.com/profile.jpg"
        );

        // when
        User created = userService.createUser(command);

        // then
        assertThat(created.getId()).isNotNull();
        assertThat(created.getSocialId()).isEqualTo(uniqueSocialId);
        assertThat(created.getNickname()).isEqualTo(uniqueNickname);
        assertThat(created.getSocialAccount()).isEqualTo(SocialAccount.KAKAO);
        assertThat(created.getProfileImage()).isEqualTo("https://example.com/profile.jpg");
    }

    @Test
    @DisplayName("프로필 업데이트 성공")
    void updateProfile_success() {
        // given
        String uniqueSocialId = UUID.randomUUID().toString();
        String uniqueNickname = "기존닉네임_" + UUID.randomUUID().toString().substring(0, 8);
        String newNickname = "새닉네임_" + UUID.randomUUID().toString().substring(0, 8);
        User user = User.builder()
                .socialId(uniqueSocialId)
                .nickname(uniqueNickname)
                .socialAccount(SocialAccount.KAKAO)
                .profileImage("https://example.com/old.jpg")
                .build();
        userRepository.save(user);

        // when
        userService.updateProfile(user, newNickname, "https://example.com/new.jpg");

        // then
        assertThat(user.getNickname()).isEqualTo(newNickname);
        assertThat(user.getProfileImage()).isEqualTo("https://example.com/new.jpg");
    }

    @Test
    @DisplayName("프로필 이미지만 업데이트 성공 - 닉네임은 유지")
    void updateProfileImage_success() {
        // given
        String uniqueSocialId = UUID.randomUUID().toString();
        String uniqueNickname = "기존닉네임_" + UUID.randomUUID().toString().substring(0, 8);
        User user = User.builder()
                .socialId(uniqueSocialId)
                .nickname(uniqueNickname)
                .socialAccount(SocialAccount.KAKAO)
                .profileImage("https://example.com/old.jpg")
                .build();
        userRepository.save(user);

        // when
        userService.updateProfileImage(user, "https://example.com/new.jpg");

        // then
        assertThat(user.getNickname()).isEqualTo(uniqueNickname);
        assertThat(user.getProfileImage()).isEqualTo("https://example.com/new.jpg");
    }

    @Test
    @DisplayName("유니크 닉네임 생성 성공")
    void generateUniqueNickname_success() {
        // when
        String nickname = userService.generateUniqueNickname();

        // then
        assertThat(nickname).isNotNull();
        assertThat(nickname).matches("^.+_\\d{4}$");
        assertThat(userRepository.existsByNickname(nickname)).isFalse();
    }

    @Test
    @DisplayName("유니크 닉네임 생성 - 중복되지 않는 닉네임 반환")
    void generateUniqueNickname_returnsUniqueNickname() {
        // when
        String nickname1 = userService.generateUniqueNickname();

        // 첫 번째 닉네임으로 사용자 생성
        userRepository.save(User.builder()
                .socialId("user1")
                .nickname(nickname1)
                .socialAccount(SocialAccount.KAKAO)
                .build());

        String nickname2 = userService.generateUniqueNickname();

        // then
        assertThat(nickname2).isNotEqualTo(nickname1);
    }
}
