
package com.example.documentstorage.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CursorService {

    private final byte[] secret;

    public CursorService(
            @Value("${app.documents.cursor-signing-secret}")
            String secret) {

        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "DOCUMENT_CURSOR_SIGNING_SECRET must be at least 32 characters");
        }

        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String encode(String value) {
        String payload = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));

        String signature = sign(payload);
        return payload + "." + signature;
    }

    public String decode(String cursor) {
        try {
            String[] parts = cursor.split("\\.", -1);

            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid cursor");
            }

            String expectedSignature = sign(parts[0]);

            if (!MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.US_ASCII),
                    parts[1].getBytes(StandardCharsets.US_ASCII))) {
                throw new IllegalArgumentException("Invalid cursor");
            }

            return new String(
                    Base64.getUrlDecoder().decode(parts[0]),
                    StandardCharsets.UTF_8);

        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid or tampered cursor", exception);
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));

            byte[] signature = mac.doFinal(
                    payload.getBytes(StandardCharsets.US_ASCII));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(signature);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to sign pagination cursor", exception);
        }
    }
}
