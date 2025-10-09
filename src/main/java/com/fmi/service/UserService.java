package com.fmi.service;

import com.fmi.domain.User;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User findUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
    }
}
