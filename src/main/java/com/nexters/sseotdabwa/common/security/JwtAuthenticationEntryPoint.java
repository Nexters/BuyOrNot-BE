package com.nexters.sseotdabwa.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexters.sseotdabwa.api.auth.exception.AuthErrorCode;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * JWT 인증 실패 시 처리하는 EntryPoint
 * - 인증되지 않은 요청이 보호된 리소스에 접근할 때 401 응답 반환
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        AuthErrorCode errorCode = AuthErrorCode.AUTHENTICATION_REQUIRED;

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> errorResponse = ApiResponse.error(
                errorCode.getMessage(),
                errorCode.getHttpStatus(),
                errorCode.getCode()
        );

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
