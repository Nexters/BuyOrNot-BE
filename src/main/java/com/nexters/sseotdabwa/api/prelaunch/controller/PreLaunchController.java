package com.nexters.sseotdabwa.api.prelaunch.controller;

import com.nexters.sseotdabwa.api.prelaunch.dto.PreLaunchEmailRequest;
import com.nexters.sseotdabwa.api.prelaunch.dto.PreLaunchEmailResponse;
import com.nexters.sseotdabwa.api.prelaunch.facade.PreLaunchFacade;
import com.nexters.sseotdabwa.common.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pre-launch")
@RequiredArgsConstructor
public class PreLaunchController implements PreLaunchControllerSpec {

    private final PreLaunchFacade preLaunchFacade;

    @Override
    @PostMapping("/emails")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PreLaunchEmailResponse> registerEmail(
            @Valid @RequestBody PreLaunchEmailRequest request
    ) {
        PreLaunchEmailResponse response = preLaunchFacade.registerEmail(request);
        return ApiResponse.success(response, HttpStatus.CREATED);
    }
}
