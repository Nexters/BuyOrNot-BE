package com.nexters.sseotdabwa.common.security;

import com.nexters.sseotdabwa.api.users.exception.UserErrorCode;
import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.users.entity.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 닉네임 미설정(nickname == null) 회원의 우회 진입을 막는다.
 * FE 라우팅 가드만으로는 인증 없는 직접 API 호출을 막을 수 없어 서버 레벨에서도 강제한다.
 * 화이트리스트(프로필 설정, 내 정보 조회, 로그아웃) 외 모든 인증된 요청을 차단(NICKNAME_REQUIRED).
 * 비인증 요청(principal이 User가 아님)은 이 인터셉터의 관심사가 아니므로 그대로 통과시킨다.
 */
@Component
public class NicknameRequiredInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return true;
        }
        if (user.getNickname() != null) {
            return true;
        }
        if (isWhitelisted(request)) {
            return true;
        }
        throw new GlobalException(UserErrorCode.NICKNAME_REQUIRED);
    }

    private boolean isWhitelisted(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();

        return ("GET".equals(method) && "/api/v1/users/me".equals(uri))
                || ("PATCH".equals(method) && "/api/v1/users/me/profile".equals(uri))
                || ("POST".equals(method) && "/api/v1/auth/logout".equals(uri));
    }
}
