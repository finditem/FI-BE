package com.fmi.domain.user.service.internal;

import com.fmi.domain.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NicknameValidator {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 10;
    private static final Pattern VALID_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9]+$");

    private final Set<String> bannedWords = new HashSet<>();
    private final UserRepository userRepository;

    @PostConstruct
    public void init() {
        loadBannedWords();
    }

    public ValidationResult validate(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return ValidationResult.invalid("닉네임을 입력해주세요.");
        }

        String trimmed = nickname.trim();
        if (trimmed.length() < MIN_LENGTH) {
            return ValidationResult.invalid("닉네임은 최소 " + MIN_LENGTH + "자 이상이어야 합니다.");
        }
        if (trimmed.length() > MAX_LENGTH) {
            return ValidationResult.invalid("닉네임은 최대 " + MAX_LENGTH + "자까지 가능합니다.");
        }
        if (!VALID_PATTERN.matcher(trimmed).matches()) {
            return ValidationResult.invalid("닉네임은 한글, 영문, 숫자만 사용 가능합니다.");
        }

        String lowerNickname = trimmed.toLowerCase();
        for (String bannedWord : bannedWords) {
            if (lowerNickname.contains(bannedWord)) {
                return ValidationResult.invalid("사용할 수 없는 닉네임입니다.");
            }
        }
        return ValidationResult.success();
    }

    public ValidationResult validateAvailable(String nickname) {
        ValidationResult validationResult = validate(nickname);
        if (!validationResult.valid()) {
            return validationResult;
        }
        if (userRepository.existsByNickname(nickname.trim())) {
            return ValidationResult.duplicate("중복된 닉네임입니다.");
        }
        return ValidationResult.success();
    }

    private void loadBannedWords() {
        try {
            ClassPathResource resource = new ClassPathResource("banned-words.txt");
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
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

    public record ValidationResult(boolean valid, Failure failure, String errorMessage) {

        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, Failure.INVALID, errorMessage);
        }

        public static ValidationResult duplicate(String errorMessage) {
            return new ValidationResult(false, Failure.DUPLICATE, errorMessage);
        }
    }

    public enum Failure {
        INVALID,
        DUPLICATE
    }
}
