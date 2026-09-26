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
        String email = "test@example.com";
        UserCreateCommand command = new UserCreateCommand(
                uniqueSocialId,
                uniqueNickname,
                SocialAccount.KAKAO,
                "https://example.com/profile.jpg",
                email
        );

        // when
        User created = userService.createUser(command);

        // then
        assertThat(created.getId()).isNotNull();
        assertThat(created.getSocialId()).isEqualTo(uniqueSocialId);
        assertThat(created.getNickname()).isEqualTo(uniqueNickname);
        assertThat(created.getSocialAccount()).isEqualTo(SocialAccount.KAKAO);
        assertThat(created.getProfileImage()).isEqualTo("https://example.com/profile.jpg");
        assertThat(created.getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("사용자 생성 성공 - email이 null인 경우")
    void createUser_success_withNullEmail() {
        // given
        String uniqueSocialId = UUID.randomUUID().toString();
        String uniqueNickname = "테스트_" + UUID.randomUUID().toString().substring(0, 8);
        UserCreateCommand command = new UserCreateCommand(
                uniqueSocialId,
                uniqueNickname,
                SocialAccount.KAKAO,
                "https://example.com/profile.jpg",
                null
        );

        // when
        User created = userService.createUser(command);

        // then
        assertThat(created.getId()).isNotNull();
        assertThat(created.getEmail()).isNull();
    }

    @Test
    @DisplayName("프로필 업데이트 성공 - 닉네임/이미지 둘 다 변경")
    void updateProfile_success() {
        // given
        User user = createUserWithNickname("기존닉네임" + randomSuffix());
        String newNickname = "새닉네임" + randomSuffix();

        // when
        userService.updateProfile(user, newNickname, "https://example.com/new.jpg");

        // then
        assertThat(user.getNickname()).isEqualTo(newNickname);
        assertThat(user.getProfileImage()).isEqualTo("https://example.com/new.jpg");
        assertThat(user.getNicknameUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("닉네임 최초 설정 - 미설정(null) 상태에서는 쿨다운 없이 통과")
    void updateProfile_initialNicknameSetting_skipsCooldown() {
        // given
        User user = createUserWithNickname(null);

        // when
        userService.updateProfile(user, "첫닉네임" + randomSuffix(), null);

        // then
        assertThat(user.getNickname()).isNotNull();
        assertThat(user.getNicknameUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("닉네임을 현재 값과 동일하게(대소문자만 다르게) 요청하면 변경 없이 통과")
    void updateProfile_sameNicknameIgnoringCase_noop() {
        // given
        String nickname = "same" + randomSuffix();
        User user = createUserWithNickname(nickname);

        // when
        userService.updateProfile(user, nickname.toUpperCase(), null);

        // then
        assertThat(user.getNickname()).isEqualTo(nickname);
        assertThat(user.getNicknameUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("닉네임에 특수문자 포함 시 USER_007 예외")
    void updateProfile_specialCharacter_throwsUser007() {
        // given
        User user = createUserWithNickname("기존닉네임" + randomSuffix());

        // when & then
        assertThatThrownBy(() -> userService.updateProfile(user, "닉네임!", null))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.NICKNAME_SPECIAL_CHARACTER);
    }

    @Test
    @DisplayName("닉네임에 중간 공백 포함 시 USER_008 예외")
    void updateProfile_whitespace_throwsUser008() {
        // given
        User user = createUserWithNickname("기존닉네임" + randomSuffix());

        // when & then
        assertThatThrownBy(() -> userService.updateProfile(user, "닉 네임", null))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.NICKNAME_WHITESPACE);
    }

    @Test
    @DisplayName("닉네임이 3자 미만이면 USER_009 예외")
    void updateProfile_tooShort_throwsUser009() {
        // given
        User user = createUserWithNickname("기존닉네임" + randomSuffix());

        // when & then
        assertThatThrownBy(() -> userService.updateProfile(user, "가나", null))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.NICKNAME_LENGTH_INVALID);
    }

    @Test
    @DisplayName("닉네임이 숫자로만 구성되면 USER_010 예외")
    void updateProfile_digitsOnly_throwsUser010() {
        // given
        User user = createUserWithNickname("기존닉네임" + randomSuffix());

        // when & then
        assertThatThrownBy(() -> userService.updateProfile(user, "123456", null))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.NICKNAME_INVALID_COMPOSITION);
    }

    @Test
    @DisplayName("닉네임이 자음/모음 단독으로 구성되면 USER_010 예외")
    void updateProfile_standaloneJamo_throwsUser010() {
        // given
        User user = createUserWithNickname("기존닉네임" + randomSuffix());

        // when & then
        assertThatThrownBy(() -> userService.updateProfile(user, "ㅋㅋㅋ해요", null))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.NICKNAME_INVALID_COMPOSITION);
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임(대소문자 무관)으로 변경 시 USER_011 예외")
    void updateProfile_duplicateNickname_throwsUser011() {
        // given
        String takenNickname = "이미있는닉네임" + randomSuffix();
        createUserWithNickname(takenNickname);
        User user = createUserWithNickname("기존닉네임" + randomSuffix());

        // when & then
        assertThatThrownBy(() -> userService.updateProfile(user, takenNickname.toUpperCase(), null))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.NICKNAME_DUPLICATE);
    }

    @Test
    @DisplayName("사칭 키워드가 포함되면 USER_012 예외")
    void updateProfile_forbiddenWord_throwsUser012() {
        // given
        User user = createUserWithNickname("기존닉네임" + randomSuffix());

        // when & then
        assertThatThrownBy(() -> userService.updateProfile(user, "공식운영자", null))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.NICKNAME_FORBIDDEN_WORD);
    }

    @Test
    @DisplayName("욕설이 포함되면 USER_012 예외 (댓글 금칙어 필터 재사용)")
    void updateProfile_profanity_throwsUser012() {
        // given
        User user = createUserWithNickname("기존닉네임" + randomSuffix());

        // when & then
        assertThatThrownBy(() -> userService.updateProfile(user, "썅놈아", null))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.NICKNAME_FORBIDDEN_WORD);
    }

    @Test
    @DisplayName("마지막 변경일로부터 20일 이내 재변경 시도 시 USER_013 예외")
    void updateProfile_withinCooldown_throwsUser013() {
        // given
        User user = createUserWithNickname("기존닉네임" + randomSuffix());
        userService.updateProfile(user, "한번바꾼닉네임" + randomSuffix(), null);

        // when & then
        assertThatThrownBy(() -> userService.updateProfile(user, "또바꾼닉네임" + randomSuffix(), null))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.NICKNAME_CHANGE_COOLDOWN);
    }

    @Test
    @DisplayName("프로필 이미지만 변경 시 nicknameUpdatedAt은 갱신되지 않는다")
    void updateProfile_imageOnly_doesNotTouchNicknameUpdatedAt() {
        // given
        User user = createUserWithNickname("기존닉네임" + randomSuffix());

        // when
        userService.updateProfile(user, null, "https://example.com/new.jpg");

        // then
        assertThat(user.getProfileImage()).isEqualTo("https://example.com/new.jpg");
        assertThat(user.getNicknameUpdatedAt()).isNull();
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
    @DisplayName("사용자 삭제 성공")
    void delete_success() {
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
        userService.delete(savedUser);

        // then
        assertThat(userRepository.findById(savedUser.getId())).isEmpty();
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

    @Test
    @DisplayName("FCM 토큰 저장/갱신 성공 - fcmToken 업데이트")
    void updateFcmToken_success() {
        // given
        User user = userRepository.saveAndFlush(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname("테스트_" + UUID.randomUUID().toString().substring(0, 8))
                .socialAccount(SocialAccount.KAKAO)
                .build());

        String token = "fcm_" + UUID.randomUUID();

        // when
        userService.updateFcmToken(user.getId(), token);

        // then
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getFcmToken()).isEqualTo(token);
    }

    private User createUserWithNickname(String nickname) {
        return userRepository.save(User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname(nickname)
                .socialAccount(SocialAccount.KAKAO)
                .build());
    }

    private String randomSuffix() {
        return UUID.randomUUID().toString().substring(0, 3);
    }
}
