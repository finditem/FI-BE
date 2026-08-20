package com.fmi.domain.auth.service;

import com.fmi.domain.auth.repository.UserRepository;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NicknameGeneratorService {

    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    private static final List<String> ADJECTIVES = Arrays.asList("길찾는", "지도보는", "탐색하는", "이동중인", "확인중인", "대기중인");

    private static final List<String> ANIMALS = Arrays.asList("토끼", "판다", "오리", "펭귄", "사슴", "라쿤", "곰");

    public String generateRandomNickname() {
        String nickname;
        int attempts = 0;
        int maxAttempts = 50;

        do {
            nickname = generateNickname();
            attempts++;

            if (attempts >= maxAttempts) {
                nickname = generateFallbackNickname();
                break;
            }
        } while (userRepository.existsByNickname(nickname));

        return nickname;
    }

    private String generateNickname() {
        String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
        String animal = ANIMALS.get(random.nextInt(ANIMALS.size()));
        int number = random.nextInt(999) + 1; // 1~999
        return adjective + animal + number;
    }

    private String generateFallbackNickname() {
        String base = "찾아줘";
        String randomCode;
        int attempts = 0;
        int maxAttempts = 100;

        do {
            randomCode = generateRandomCode(6);
            attempts++;

            if (attempts >= maxAttempts) {
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
