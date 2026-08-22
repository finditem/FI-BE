package com.fmi.service;

import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserQueryService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User findUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public User findUserIfNullReturnNull(UserDetails userDetails) {
        if (Objects.isNull(userDetails)) {
            return null;
        }

        return userRepository.findByEmail(userDetails.getUsername()).orElse(null);
    }
}
