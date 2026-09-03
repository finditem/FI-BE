package com.fmi.domain.user.service.internal;

import com.fmi.domain.user.repository.UserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
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

    public void validate(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new GeneralException(ErrorStatus._INVALID_NICKNAME);
        }

        String trimmed = nickname.trim();
        if (trimmed.length() < MIN_LENGTH) {
            throw new GeneralException(ErrorStatus._INVALID_NICKNAME);
        }
        if (trimmed.length() > MAX_LENGTH) {
            throw new GeneralException(ErrorStatus._INVALID_NICKNAME);
        }
        if (!VALID_PATTERN.matcher(trimmed).matches()) {
            throw new GeneralException(ErrorStatus._INVALID_NICKNAME);
        }

        String lowerNickname = trimmed.toLowerCase();
        for (String bannedWord : bannedWords) {
            if (lowerNickname.contains(bannedWord)) {
                throw new GeneralException(ErrorStatus._INVALID_NICKNAME);
            }
        }
    }

    public void validateAvailable(String nickname) {
        validate(nickname);
        if (userRepository.existsByNickname(nickname.trim())) {
            throw new GeneralException(ErrorStatus._NICKNAME_DUPLICATED);
        }
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
}
