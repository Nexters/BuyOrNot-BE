package com.nexters.sseotdabwa.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * 현재 인증된 사용자를 컨트롤러 파라미터로 주입받기 위한 어노테이션
 * - @AuthenticationPrincipal 어노테이션을 커스터마이징한 것
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@AuthenticationPrincipal
public @interface CurrentUser {

}
