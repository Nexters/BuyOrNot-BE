package com.nexters.sseotdabwa.api.prelaunch.facade;

import com.nexters.sseotdabwa.api.prelaunch.dto.PreLaunchEmailRequest;
import com.nexters.sseotdabwa.api.prelaunch.dto.PreLaunchEmailResponse;
import com.nexters.sseotdabwa.domain.prelaunch.entity.PreLaunchEmail;
import com.nexters.sseotdabwa.domain.prelaunch.service.PreLaunchEmailService;
import com.nexters.sseotdabwa.domain.prelaunch.service.command.PreLaunchEmailCreateCommand;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PreLaunchFacade {

    private final PreLaunchEmailService preLaunchEmailService;

    @Transactional
    public PreLaunchEmailResponse registerEmail(PreLaunchEmailRequest request) {
        PreLaunchEmailCreateCommand command = new PreLaunchEmailCreateCommand(request.email());
        PreLaunchEmail preLaunchEmail = preLaunchEmailService.createEmail(command);
        return PreLaunchEmailResponse.from(preLaunchEmail);
    }
}
