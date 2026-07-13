
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
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
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

    @Value("${app.otp.log-enabled:true}")
    private boolean otpLogEnabled;

    @Transactional
    public OtpResponse requestOtp(PhoneOtpRequest request, HttpServletRequest httpRequest) {
        String phone = normalizePhone(request.getPhone());
        String otp = generateOtp();
        log.info("OTP requested phone={} remoteIp={}", maskPhone(phone), clientIp(httpRequest));

        LoginOtp loginOtp = new LoginOtp();
        loginOtp.setPhone(phone);
        loginOtp.setOtp(otp);
        loginOtp.setExpiresAt(Instant.now().plusSeconds(1000000));
        loginOtpRepository.save(loginOtp);

        if (otpLogEnabled) {
            System.out.println("TEST OTP for " + phone + " is " + otp);
            log.info("TEST OTP generated phone={} otp={} expiresAt={}", maskPhone(phone), otp, loginOtp.getExpiresAt());
        } else {
            log.info("OTP generated phone={} expiresAt={}", maskPhone(phone), loginOtp.getExpiresAt());
        }

        String message = otpLogEnabled
                ? "OTP generated. Check server console in test mode."
                : "OTP generated.";
        return new OtpResponse(phone, 300, message);
    }

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request, HttpServletRequest httpRequest) {
        String phone = normalizePhone(request.getPhone());
        log.info("OTP verification started phone={} remoteIp={}", maskPhone(phone), clientIp(httpRequest));
        LoginOtp loginOtp = loginOtpRepository.findFirstByPhoneAndUsedFalseOrderByCreatedAtDesc(phone)
                .orElseThrow(() -> new IllegalArgumentException("OTP not found"));

        if (loginOtp.getExpiresAt().isBefore(Instant.now())) {
            log.warn("OTP verification failed reason=expired phone={} otpId={}", maskPhone(phone), loginOtp.getId());
            throw new IllegalArgumentException("OTP expired");
        }

        if (!loginOtp.getOtp().equals(request.getOtp())) {
            log.warn("OTP verification failed reason=invalid phone={} otpId={}", maskPhone(phone), loginOtp.getId());
            throw new IllegalArgumentException("Invalid OTP");
        }

        loginOtp.setUsed(true);
        loginOtpRepository.save(loginOtp);

        User user = userRepository.findByPhone(phone)
                .orElseGet(() -> createPhoneUser(phone, request));
        walletService.createStartingWallet(user);
        log.info("OTP verification succeeded phone={} userId={} username={}", maskPhone(phone), user.getId(), user.getUsername());
        return issueTokens(user);
    }

    @Transactional
    public List<UserSummary> getTestUsers() {
        requireTestUsersEnabled();
        log.info("Ensuring test users");
        return ensureTestUsers()
                .stream()
                .map(UserSummary::from)
                .toList();
    }

    @Transactional
    public List<AuthResponse> getTestUserSessions() {
        requireTestUsersEnabled();
        log.info("Issuing test user sessions");
        return ensureTestUsers().stream()
                .map(this::issueTokens)
                .toList();
    }

    public AuthResponse refresh(String token,
            HttpServletRequest httpRequest) {
        log.info("Refresh token requested remoteIp={}", clientIp(httpRequest));

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
        log.info("Refresh token succeeded userId={} username={}", stored.getUser().getId(), stored.getUser().getUsername());
        return new AuthResponse(newAccess, token, UserSummary.from(stored.getUser()));
    }

    public void logout(String token,
            HttpServletRequest httpRequest) {
        log.info("Logout requested remoteIp={}", clientIp(httpRequest));

        RefreshToken stored = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        log.info("Logout completed userId={} username={}", stored.getUser().getId(), stored.getUser().getUsername());
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
        User savedUser = userRepository.save(user);
        log.info("Created phone user userId={} username={} phone={}", savedUser.getId(), savedUser.getUsername(), maskPhone(phone));
        return savedUser;
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getUsername());
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(7 * 24 * 60 * 60));
        refreshTokenRepository.save(refreshToken);
        log.info("Issued auth tokens userId={} username={} refreshTokenId={}",
                user.getId(), user.getUsername(), refreshToken.getId());
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

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "-";
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) {
            return "****";
        }
        return "****" + phone.substring(phone.length() - 4);
    }

    private record TestUserSpec(String username, String phone, String nickname, String avatarUrl) {
    }
}
