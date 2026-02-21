package com.nexters.sseotdabwa.domain.prelaunch.service;

import com.nexters.sseotdabwa.common.exception.GlobalException;
import com.nexters.sseotdabwa.domain.prelaunch.entity.PreLaunchEmail;
import com.nexters.sseotdabwa.domain.prelaunch.exception.PreLaunchErrorCode;
import com.nexters.sseotdabwa.domain.prelaunch.repository.PreLaunchEmailRepository;
import com.nexters.sseotdabwa.domain.prelaunch.service.command.PreLaunchEmailCreateCommand;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreLaunchEmailService {

    private final PreLaunchEmailRepository preLaunchEmailRepository;

    @Transactional
    public PreLaunchEmail createEmail(PreLaunchEmailCreateCommand command) {
        if (preLaunchEmailRepository.existsByEmail(command.email())) {
            throw new GlobalException(PreLaunchErrorCode.PRELAUNCH_EMAIL_ALREADY_EXISTS);
        }

        PreLaunchEmail preLaunchEmail = PreLaunchEmail.builder()
                .email(command.email())
                .build();

        return preLaunchEmailRepository.save(preLaunchEmail);
    }
}
