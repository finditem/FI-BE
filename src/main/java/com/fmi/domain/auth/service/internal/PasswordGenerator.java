package com.fmi.domain.auth.service.internal;

import java.util.Random;
import org.springframework.stereotype.Component;

@Component
public class PasswordGenerator {

    public String generateTemporaryPassword() {
        String characters = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 12; i++) {
            password.append(characters.charAt(random.nextInt(characters.length())));
        }

        return password.toString();
    }
}
