package com.fmi.utils;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public final class IpUtil {
    public static String getClientIp(HttpServletRequest request) {
        if (Objects.isNull(request)) return "unknown";

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (Objects.nonNull(xForwardedFor) && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (Objects.nonNull(xRealIp) && !xRealIp.isBlank()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    public static String hashIp(String clientIp) {
        if (Objects.isNull(clientIp) || clientIp.isBlank()) {
            return "unknown";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(clientIp.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String s = Integer.toHexString(0xff & b);
                if (s.length() == 1) hex.append('0');
                hex.append(s);
            }
            return hex.toString();

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다.", exception);
        }
    }

    private IpUtil() {
    }
}
