package com.nexters.sseotdabwa.common;
import com.nexters.sseotdabwa.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class RootController {

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("ok", "헬스 체크 성공", HttpStatus.OK);
    }
}
