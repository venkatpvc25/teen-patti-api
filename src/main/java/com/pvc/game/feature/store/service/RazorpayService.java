package com.pvc.game.feature.store.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pvc.game.feature.store.config.RazorpayProperties;
import com.pvc.game.feature.store.entity.Purchase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RazorpayService {

    private static final URI ORDERS_URI = URI.create("https://api.razorpay.com/v1/orders");

    private final RazorpayProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String createOrder(Purchase purchase) {
        requireConfigured();
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "amount", purchase.getAmountPaise(),
                    "currency", purchase.getCurrency(),
                    "receipt", purchase.getId().toString(),
                    "notes", Map.of(
                            "purchaseId", purchase.getId().toString(),
                            "shopItemId", purchase.getShopItem().getId().toString())));

            HttpRequest request = HttpRequest.newBuilder(ORDERS_URI)
                    .header("Authorization", basicAuth())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Razorpay order creation failed: " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            return json.get("id").asText();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create Razorpay order", exception);
        }
    }

    public boolean isValidSignature(String orderId, String paymentId, String signature) {
        requireConfigured();
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getKeySecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = toHex(digest);
            return constantTimeEquals(expected, signature);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to verify Razorpay signature", exception);
        }
    }

    public String keyId() {
        requireConfigured();
        return properties.getKeyId();
    }

    private void requireConfigured() {
        if (isBlank(properties.getKeyId()) || isBlank(properties.getKeySecret())) {
            throw new IllegalStateException("Razorpay credentials are not configured");
        }
    }

    private String basicAuth() {
        String credentials = properties.getKeyId() + ":" + properties.getKeySecret();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private boolean constantTimeEquals(String left, String right) {
        if (right == null || left.length() != right.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length(); i++) {
            result |= left.charAt(i) ^ right.charAt(i);
        }
        return result == 0;
    }
}
