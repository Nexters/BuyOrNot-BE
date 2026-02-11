package com.nexters.sseotdabwa.domain.auth.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nexters.sseotdabwa.domain.auth.entity.RefreshToken;
import com.nexters.sseotdabwa.domain.auth.repository.RefreshTokenRepository;
import com.nexters.sseotdabwa.domain.auth.service.command.RefreshTokenCreateCommand;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshToken save(RefreshTokenCreateCommand command) {
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(command.userId())
                .token(command.token())
                .expiresAt(command.expiresAt())
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    public void deleteByUserId(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
