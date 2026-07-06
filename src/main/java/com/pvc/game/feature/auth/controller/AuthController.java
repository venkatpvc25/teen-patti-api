package com.pvc.game.feature.auth.controller;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import com.pvc.game.feature.auth.service.AuthService;
import com.pvc.game.comman.response.ApiResponse;
import com.pvc.game.feature.auth.dto.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/otp/request")
    public ApiResponse<OtpResponse> requestOtp(@Valid @RequestBody PhoneOtpRequest request,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(service.requestOtp(request, httpRequest));
    }

    @PostMapping("/otp/verify")
    public ApiResponse<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(service.verifyOtp(request, httpRequest));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@RequestParam String token,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(service.refresh(token, httpRequest));
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout(@RequestParam String token,
            HttpServletRequest httpRequest) {
        service.logout(token, httpRequest);
        return ApiResponse.ok("Logged out successfully");
    }

    @GetMapping("/test-users")
    public List<UserSummary> getTestUsers() {
        return service.getTestUsers();
    }

    @GetMapping("/test-users/sessions")
    public ApiResponse<List<AuthResponse>> getTestUserSessions() {
        return ApiResponse.ok(service.getTestUserSessions());
    }
}
