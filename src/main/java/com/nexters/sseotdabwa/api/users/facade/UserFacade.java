package com.nexters.sseotdabwa.api.users.facade;

import org.springframework.stereotype.Component;

import com.nexters.sseotdabwa.api.users.dto.UserResponse;
import com.nexters.sseotdabwa.domain.users.entity.User;

/**
 * 사용자 관련 비즈니스 로직을 조합하는 Facade
 */
@Component
public class UserFacade {

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    public UserResponse getMyInfo(User user) {
        return UserResponse.from(user);
    }
}
