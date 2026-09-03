package com.fmi.domain.user.service.internal;

import com.fmi.domain.user.repository.UserRepository;
import java.security.SecureRandom;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NicknameGenerator {

    private static final List<String> ADJECTIVES = List.of("길찾는", "지도보는", "탐색하는", "이동중인", "확인중인", "대기중인");
    private static final List<String> ANIMALS = List.of("토끼", "판다", "오리", "펭귤", "사슴", "라쿤", "곰");
    private static final int MAX_GENERATION_ATTEMPTS = 50;
    private static final int MAX_FALLBACK_ATTEMPTS = 100;

    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        String nickname;
        int attempts = 0;

        do {
            nickname = generateCandidate();
            attempts++;
            if (attempts >= MAX_GENERATION_ATTEMPTS) {
                return generateFallback();
            }
        } while (userRepository.existsByNickname(nickname));

        return nickname;
    }

    private String generateCandidate() {
        String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
        String animal = ANIMALS.get(random.nextInt(ANIMALS.size()));
        int number = random.nextInt(999) + 1;
        return adjective + animal + number;
    }

    private String generateFallback() {
        String base = "찾아줘";
        String randomCode;
        int attempts = 0;

        do {
            randomCode = generateRandomCode(6);
            attempts++;
            if (attempts >= MAX_FALLBACK_ATTEMPTS) {
                randomCode = generateRandomCode(8) + System.currentTimeMillis() % 1000;
                break;
            }
        } while (userRepository.existsByNickname(base + "_" + randomCode));

        return base + "_" + randomCode;
    }

    private String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
}
