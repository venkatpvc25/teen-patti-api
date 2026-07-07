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
    private static final URI CONTACTS_URI = URI.create("https://api.razorpay.com/v1/contacts");
    private static final URI FUND_ACCOUNTS_URI = URI.create("https://api.razorpay.com/v1/fund_accounts");
    private static final URI PAYOUTS_URI = URI.create("https://api.razorpay.com/v1/payouts");

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

    public String createContact(String name, String phone, String userId) {
        requireConfigured();
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "name", fallback(name, "Game user"),
                    "contact", fallback(phone, "0000000000"),
                    "type", "customer",
                    "reference_id", userId));
            JsonNode json = post(CONTACTS_URI, requestBody, "Razorpay contact creation failed");
            return json.get("id").asText();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create Razorpay contact", exception);
        }
    }

    public String createUpiFundAccount(String contactId, String upiId) {
        requireConfigured();
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "contact_id", contactId,
                    "account_type", "vpa",
                    "vpa", Map.of("address", upiId)));
            JsonNode json = post(FUND_ACCOUNTS_URI, requestBody, "Razorpay fund account creation failed");
            return json.get("id").asText();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create Razorpay fund account", exception);
        }
    }

    public String createPayout(String fundAccountId, long amountPaise, String referenceId, String narration) {
        requirePayoutConfigured();
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "account_number", properties.getAccountNumber(),
                    "fund_account_id", fundAccountId,
                    "amount", amountPaise,
                    "currency", "INR",
                    "mode", "UPI",
                    "purpose", "payout",
                    "queue_if_low_balance", true,
                    "reference_id", referenceId,
                    "narration", narration,
                    "notes", Map.of("withdrawalId", referenceId)));
            JsonNode json = post(PAYOUTS_URI, requestBody, "Razorpay payout creation failed");
            return json.get("id").asText();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create Razorpay payout", exception);
        }
    }

    public String keyId() {
        requireConfigured();
        return properties.getKeyId();
    }

    public long withdrawalPaiseFor(long chips) {
        if (chips <= 0) {
            throw new IllegalArgumentException("Withdrawal chips must be greater than zero");
        }
        return Math.multiplyExact(chips, properties.getWithdrawalPaisePerChip());
    }

    private void requireConfigured() {
        if (isBlank(properties.getKeyId()) || isBlank(properties.getKeySecret())) {
            throw new IllegalStateException("Razorpay credentials are not configured");
        }
    }

    private void requirePayoutConfigured() {
        requireConfigured();
        if (isBlank(properties.getAccountNumber())) {
            throw new IllegalStateException("Razorpay account number is not configured");
        }
    }

    private JsonNode post(URI uri, String body, String errorPrefix) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Authorization", basicAuth())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(errorPrefix + ": " + response.body());
        }
        return objectMapper.readTree(response.body());
    }

    private String basicAuth() {
        String credentials = properties.getKeyId() + ":" + properties.getKeySecret();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private String fallback(String value, String fallback) {
        return isBlank(value) ? fallback : value;
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
