package com.nexters.sseotdabwa.api.prelaunch.dto;

import com.nexters.sseotdabwa.domain.prelaunch.entity.PreLaunchEmail;

public record PreLaunchEmailResponse(
        Long id,
        String email
) {
    public static PreLaunchEmailResponse from(PreLaunchEmail preLaunchEmail) {
        return new PreLaunchEmailResponse(
                preLaunchEmail.getId(),
                preLaunchEmail.getEmail()
        );
    }
}
