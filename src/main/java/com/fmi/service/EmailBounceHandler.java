package com.fmi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 이메일 Bounce Back 처리 서비스
 * 
 * 주의: 실제 bounce back 처리를 위해서는 Gmail API 또는 IMAP 설정이 필요합니다.
 * 현재는 기본 구조만 제공합니다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailBounceHandler {

    private final StringRedisTemplate redis;

    private static final String BOUNCE_KEY_PREFIX = "email:bounce:";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");

    /**
     * Bounce back 이메일을 처리합니다.
     * 
     * @param bounceEmailContent bounce back 이메일의 내용
     */
    public void handleBounceBack(String bounceEmailContent) {
        // 원본 수신자 이메일 주소 추출
        String recipientEmail = extractRecipientEmail(bounceEmailContent);
        
        if (recipientEmail == null) {
            log.warn("⚠️ [BOUNCE BACK] 원본 수신자 이메일 주소를 추출할 수 없습니다.");
            return;
        }

        // Bounce back 발생한 이메일 주소를 Redis에 저장 (24시간)
        String bounceKey = BOUNCE_KEY_PREFIX + recipientEmail;
        redis.opsForValue().set(bounceKey, "true", Duration.ofHours(24));
        
        log.warn("❌ [BOUNCE BACK] email={} - 이메일 주소가 유효하지 않거나 존재하지 않습니다.", recipientEmail);
    }

    /**
     * Bounce back 이메일 내용에서 원본 수신자 이메일 주소를 추출합니다.
     * 
     * @param bounceContent bounce back 이메일 내용
     * @return 원본 수신자 이메일 주소, 추출 실패 시 null
     */
    private String extractRecipientEmail(String bounceContent) {
        // Bounce back 이메일에서 원본 수신자 이메일 주소를 찾는 패턴
        // 실제 bounce back 형식은 이메일 서버마다 다르므로 여러 패턴을 시도
        
        // 패턴 1: "To: <email@example.com>" 형식
        Pattern toPattern = Pattern.compile("To:\\s*<?([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,})>?", Pattern.CASE_INSENSITIVE);
        Matcher toMatcher = toPattern.matcher(bounceContent);
        if (toMatcher.find()) {
            return toMatcher.group(1);
        }

        // 패턴 2: "Original-Recipient: <email@example.com>" 형식
        Pattern originalPattern = Pattern.compile("Original-Recipient:\\s*<?([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,})>?", Pattern.CASE_INSENSITIVE);
        Matcher originalMatcher = originalPattern.matcher(bounceContent);
        if (originalMatcher.find()) {
            return originalMatcher.group(1);
        }

        // 패턴 3: 일반적인 이메일 주소 패턴 (마지막 시도)
        Matcher emailMatcher = EMAIL_PATTERN.matcher(bounceContent);
        if (emailMatcher.find()) {
            return emailMatcher.group();
        }

        return null;
    }

    /**
     * 특정 이메일 주소가 bounce back을 받았는지 확인합니다.
     * 
     * @param email 확인할 이메일 주소
     * @return bounce back을 받았으면 true
     */
    public boolean hasBounced(String email) {
        String bounceKey = BOUNCE_KEY_PREFIX + email;
        String bounced = redis.opsForValue().get(bounceKey);
        return "true".equals(bounced);
    }

    /**
     * 특정 이메일 주소를 bounce back으로 직접 등록합니다.
     * (수동 등록용)
     * 
     * @param email bounce back으로 등록할 이메일 주소
     */
    public void registerBounce(String email) {
        String bounceKey = BOUNCE_KEY_PREFIX + email;
        redis.opsForValue().set(bounceKey, "true", Duration.ofHours(24));
        log.warn("❌ [BOUNCE BACK REGISTERED] email={} - 수동으로 bounce back 등록됨", email);
    }

    /**
     * Bounce back 기록을 삭제합니다.
     * 
     * @param email 이메일 주소
     */
    public void clearBounce(String email) {
        String bounceKey = BOUNCE_KEY_PREFIX + email;
        redis.delete(bounceKey);
        log.info("✅ [BOUNCE BACK CLEARED] email={}", email);
    }
}

