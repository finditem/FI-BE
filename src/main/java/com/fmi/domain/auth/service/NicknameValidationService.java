package com.fmi.domain.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
public class NicknameValidationService {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 10;
    
    // 한글, 영문, 숫자만 허용 (특수문자 제외)
    private static final Pattern VALID_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9]+$");
    
    private final Set<String> bannedWords = new HashSet<>();

    @PostConstruct
    public void init() {
        loadBannedWords();
    }

    private void loadBannedWords() {
        try {
            ClassPathResource resource = new ClassPathResource("banned-words.txt");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        bannedWords.add(line.toLowerCase());
                    }
                }
            }
            log.info("Loaded {} banned words", bannedWords.size());
        } catch (Exception e) {
            log.warn("Failed to load banned words file. Using empty list.", e);
        }
    }

    /**
     * 닉네임 유효성 검증
     * @return ValidationResult (valid, errorMessage)
     */
    public ValidationResult validate(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return ValidationResult.invalid("닉네임을 입력해주세요.");
        }

        String trimmed = nickname.trim();

        // 길이 검증
        if (trimmed.length() < MIN_LENGTH) {
            return ValidationResult.invalid("닉네임은 최소 " + MIN_LENGTH + "자 이상이어야 합니다.");
        }
        if (trimmed.length() > MAX_LENGTH) {
            return ValidationResult.invalid("닉네임은 최대 " + MAX_LENGTH + "자까지 가능합니다.");
        }

        // 패턴 검증 (한글, 영문, 숫자만 허용)
        if (!VALID_PATTERN.matcher(trimmed).matches()) {
            return ValidationResult.invalid("닉네임은 한글, 영문, 숫자만 사용 가능합니다.");
        }

        // 금칙어 검증
        String lowerNickname = trimmed.toLowerCase();
        for (String bannedWord : bannedWords) {
            if (lowerNickname.contains(bannedWord)) {
                return ValidationResult.invalid("사용할 수 없는 닉네임입니다.");
            }
        }

        return ValidationResult.valid();
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}

