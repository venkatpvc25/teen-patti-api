package com.pvc.game.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProductionConfigValidator implements ApplicationRunner {

    private static final String DEFAULT_JWT_SECRET = "change-this-local-secret-change-this-local-secret";

    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        requirePresent("DB_URL");
        requirePresent("DB_USER");
        requirePresent("DB_PASSWORD");
        requirePresent("JWT_SECRET");
        requirePresent("RAZORPAY_KEY_ID");
        requirePresent("RAZORPAY_KEY_SECRET");
        requirePresent("APP_CORS_ALLOWED_ORIGINS");

        requireSecureJwtSecret();
        requireProductionDatabase();
        requireProductionCors();
        requireFalse("app.test-users.enabled", "TEST_USERS_ENABLED must be false in prod");
        requireFalse("app.otp.log-enabled", "APP_OTP_LOG_ENABLED must be false in prod");
        requireFalse("springdoc.swagger-ui.enabled", "SWAGGER_ENABLED must be false in prod");
        requireFalse("app.docs.public-enabled", "APP_DOCS_PUBLIC_ENABLED must be false in prod");
    }

    private void requirePresent(String envKey) {
        String value = environment.getProperty(envKey);
        if (isBlank(value)) {
            throw new IllegalStateException(envKey + " is required in prod");
        }
    }

    private void requireSecureJwtSecret() {
        String secret = environment.getProperty("jwt.secret", "");
        if (DEFAULT_JWT_SECRET.equals(secret) || secret.length() < 64) {
            throw new IllegalStateException("JWT_SECRET must be a strong non-default value of at least 64 characters");
        }
    }

    private void requireProductionDatabase() {
        String dbUrl = environment.getProperty("spring.datasource.url", "");
        if (containsAny(dbUrl, "localhost", "127.0.0.1")) {
            throw new IllegalStateException("DB_URL must not point to localhost in prod");
        }
    }

    private void requireProductionCors() {
        List<String> origins = Arrays.stream(environment.getProperty("app.cors.allowed-origins", "").split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
        if (origins.isEmpty()) {
            throw new IllegalStateException("APP_CORS_ALLOWED_ORIGINS must include your production frontend origin");
        }
        for (String origin : origins) {
            if (!origin.startsWith("https://")) {
                throw new IllegalStateException("APP_CORS_ALLOWED_ORIGINS must use https origins in prod");
            }
            if (containsAny(origin, "localhost", "127.0.0.1", "yourdomain.com")) {
                throw new IllegalStateException("APP_CORS_ALLOWED_ORIGINS contains a non-production origin");
            }
        }
    }

    private void requireFalse(String property, String message) {
        if (environment.getProperty(property, Boolean.class, false)) {
            throw new IllegalStateException(message);
        }
    }

    private boolean containsAny(String value, String... needles) {
        String lowerValue = value.toLowerCase();
        for (String needle : needles) {
            if (lowerValue.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
