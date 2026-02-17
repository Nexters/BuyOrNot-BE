package com.nexters.sseotdabwa.api.notifications.controller;

import org.springframework.web.bind.annotation.*;

import com.nexters.sseotdabwa.api.notifications.facade.NotificationFacade;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationControllerSpec {

    private final NotificationFacade notificationFacade;

}
