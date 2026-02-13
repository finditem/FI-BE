package com.fmi.domain.auth.service;

import com.fmi.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NicknameGeneratorService {

    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    private static final List<String> ADJECTIVES = Arrays.asList(
            "귀여운", "멋진", "빠른", "느긋한", "용감한",
            "똑똑한", "친절한", "활발한", "조용한", "밝은",
            "차분한", "씩씩한", "당당한", "신나는", "상냥한",
            "재빠른", "튼튼한", "영리한", "즐거운", "행복한"
    );

    private static final List<String> ANIMALS = Arrays.asList(
            "고양이", "강아지", "토끼", "여우", "사슴",
            "판다", "코알라", "햄스터", "다람쥐", "펭귄",
            "돌고래", "고래", "거북이", "수달", "미어캣",
            "알파카", "라쿤", "늑대", "호랑이", "사자"
    );

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
