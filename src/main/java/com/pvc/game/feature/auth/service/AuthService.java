
package com.pvc.game.feature.auth.service;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.pvc.game.feature.auth.repository.*;
import com.pvc.game.feature.auth.entity.*;
import com.pvc.game.feature.auth.dto.*;
import com.pvc.game.security.JwtService;
import com.pvc.game.feature.wallet.service.WalletService;

import jakarta.servlet.http.HttpServletRequest;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final List<TestUserSpec> TEST_USERS = List.of(
            new TestUserSpec("player1", "+910000000001", "Player 1", "assets/avatars/test_player_1.png"),
            new TestUserSpec("player2", "+910000000002", "Player 2", "assets/avatars/test_player_2.png"),
            new TestUserSpec("player3", "+910000000003", "Player 3", "assets/avatars/test_player_3.png"),
            new TestUserSpec("player4", "+910000000004", "Player 4", "assets/avatars/test_player_4.png"));

    private final UserRepository userRepository;
    private final LoginOtpRepository loginOtpRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final WalletService walletService;

    @Value("${app.test-users.enabled:true}")
    private boolean testUsersEnabled;

    @Transactional
    public OtpResponse requestOtp(PhoneOtpRequest request, HttpServletRequest httpRequest) {
        String phone = normalizePhone(request.getPhone());
        String otp = generateOtp();

        LoginOtp loginOtp = new LoginOtp();
        loginOtp.setPhone(phone);
        loginOtp.setOtp(otp);
        loginOtp.setExpiresAt(Instant.now().plusSeconds(1000000));
        loginOtpRepository.save(loginOtp);

        System.out.println("TEST OTP for " + phone + " is " + otp);

        return new OtpResponse(phone, 300, "OTP generated. Check server console in test mode.");
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request, HttpServletRequest httpRequest) {
        String phone = normalizePhone(request.getPhone());
        LoginOtp loginOtp = loginOtpRepository.findFirstByPhoneAndUsedFalseOrderByCreatedAtDesc(phone)
                .orElseThrow(() -> new IllegalArgumentException("OTP not found"));

        if (loginOtp.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("OTP expired");
        }

        if (!loginOtp.getOtp().equals(request.getOtp())) {
            throw new IllegalArgumentException("Invalid OTP");
        }

        loginOtp.setUsed(true);
        loginOtpRepository.save(loginOtp);

        User user = userRepository.findByPhone(phone)
                .orElseGet(() -> createPhoneUser(phone, request));
        walletService.createStartingWallet(user);
        return issueTokens(user);
    }

    @Transactional
    public List<UserSummary> getTestUsers() {
        requireTestUsersEnabled();
        return ensureTestUsers()
                .stream()
                .map(UserSummary::from)
                .toList();
    }

    @Transactional
    public List<AuthResponse> getTestUserSessions() {
        requireTestUsersEnabled();
        return ensureTestUsers().stream()
                .map(this::issueTokens)
                .toList();
    }

    public AuthResponse refresh(String token,
            HttpServletRequest httpRequest) {

        RefreshToken stored = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (stored.isRevoked()) {
            throw new RuntimeException("Token revoked");
        }

        if (stored.getExpiryDate().isBefore(Instant.now())) {
            throw new RuntimeException("Token expired");
        }

        String newAccess = jwtService.generateAccessToken(
                stored.getUser().getUsername());
        return new AuthResponse(newAccess, token, UserSummary.from(stored.getUser()));
    }

    public void logout(String token,
            HttpServletRequest httpRequest) {

        RefreshToken stored = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
    }

    private User createPhoneUser(String phone, VerifyOtpRequest request) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        User user = new User();
        user.setUsername("user_" + suffix);
        user.setPhone(phone);
        user.setNickname(request.getNickname() == null || request.getNickname().isBlank()
                ? "Player " + phone.substring(Math.max(0, phone.length() - 4))
                : request.getNickname().trim());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setRole(Role.USER);
        return userRepository.save(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getUsername());
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(7 * 24 * 60 * 60));
        refreshTokenRepository.save(refreshToken);
        return new AuthResponse(accessToken, refreshToken.getToken(), UserSummary.from(user));
    }

    private List<User> ensureTestUsers() {
        return TEST_USERS.stream()
                .map(this::ensureTestUser)
                .sorted(Comparator.comparing(User::getUsername))
                .toList();
    }

    private User ensureTestUser(TestUserSpec spec) {
        User user = userRepository.findByUsername(spec.username())
                .or(() -> userRepository.findByPhone(spec.phone()))
                .orElseGet(User::new);
        user.setUsername(spec.username());
        user.setPhone(spec.phone());
        user.setNickname(spec.nickname());
        user.setAvatarUrl(spec.avatarUrl());
        user.setRole(Role.USER);
        User savedUser = userRepository.save(user);
        walletService.createStartingWallet(savedUser);
        return savedUser;
    }

    private void requireTestUsersEnabled() {
        if (!testUsersEnabled) {
            throw new IllegalStateException("Test users are disabled");
        }
    }

    private String normalizePhone(String phone) {
        String normalized = phone == null ? "" : phone.trim().replace(" ", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Phone is required");
        }
        return normalized;
    }

    private String generateOtp() {
        int value = new SecureRandom().nextInt(900000) + 100000;
        return String.valueOf(value);
    }

    private record TestUserSpec(String username, String phone, String nickname, String avatarUrl) {
    }
}
