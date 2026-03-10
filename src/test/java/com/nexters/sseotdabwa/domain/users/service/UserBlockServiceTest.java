package com.nexters.sseotdabwa.domain.users.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.nexters.sseotdabwa.api.users.exception.UserErrorCode;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.users.entity.User;
import com.nexters.sseotdabwa.domain.users.entity.UserBlock;
import com.nexters.sseotdabwa.domain.users.enums.SocialAccount;
import com.nexters.sseotdabwa.domain.users.repository.UserBlockRepository;
import com.nexters.sseotdabwa.domain.users.repository.UserRepository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserBlockServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @InjectMocks
    private UserBlockService userBlockService;

    private User createUser(Long id) {
        User user = User.builder()
                .socialId(UUID.randomUUID().toString())
                .nickname("테스트_" + UUID.randomUUID().toString().substring(0, 8))
                .socialAccount(SocialAccount.KAKAO)
                .build();

        // 테스트용으로 id 세팅
        ReflectionTestUtils.setField(user, "id", id);

        return user;
    }

    @Test
    @DisplayName("사용자 차단 성공")
    void blockUser_success() {

        User user = createUser(1L);
        User target = createUser(2L);

        when(userBlockRepository.existsByUserIdAndBlockedUserId(1L, 2L))
                .thenReturn(false);

        userBlockService.blockUser(user, target);

        verify(userBlockRepository).save(any(UserBlock.class));
    }

    @Test
    @DisplayName("자기 자신 차단 실패")
    void blockUser_selfBlock() {

        User user = createUser(1L);

        assertThatThrownBy(() ->
                userBlockService.blockUser(user, user)
        )
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        UserErrorCode.BLOCK_SELF_NOT_ALLOWED);

        verifyNoInteractions(userBlockRepository);
    }

    @Test
    @DisplayName("이미 차단된 사용자 차단 시 예외")
    void blockUser_alreadyBlocked() {

        User user = createUser(1L);
        User target = createUser(2L);

        when(userBlockRepository.existsByUserIdAndBlockedUserId(1L, 2L))
                .thenReturn(true);

        assertThatThrownBy(() ->
                userBlockService.blockUser(user, target)
        )
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        UserErrorCode.ALREADY_BLOCKED_USER);

        verify(userBlockRepository, never()).save(any());
    }

    @Test
    @DisplayName("차단 해제 성공")
    void unblock_success() {

        User user = createUser(1L);
        User target = createUser(2L);

        UserBlock block = UserBlock.builder()
                .user(user)
                .blockedUser(target)
                .build();

        when(userBlockRepository
                .findByUserIdAndBlockedUserId(1L, 2L))
                .thenReturn(Optional.of(block));

        userBlockService.unblock(user, 2L);

        verify(userBlockRepository).delete(block);
    }

    @Test
    @DisplayName("차단한 사용자 ID 목록 조회 - repository 위임 확인")
    void findBlockedUserIds_delegatesToRepository() {
        User user = createUser(1L);
        List<Long> expectedIds = List.of(2L, 3L);

        when(userBlockRepository.findBlockedUserIdsByUserId(1L))
                .thenReturn(expectedIds);

        List<Long> result = userBlockService.findBlockedUserIds(user.getId());

        assertThat(result).isEqualTo(expectedIds);
        verify(userBlockRepository).findBlockedUserIdsByUserId(1L);
    }

    @Test
    @DisplayName("차단 관계 없을 때 차단 해제 실패")
    void unblock_notFound() {

        User user = createUser(1L);

        when(userBlockRepository
                .findByUserIdAndBlockedUserId(1L, 2L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                userBlockService.unblock(user, 2L)
        )
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode",
                        UserErrorCode.BLOCK_USER_NOT_FOUND);

        verify(userBlockRepository, never()).delete(any());
    }
}
