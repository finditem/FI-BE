package com.fmi.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class UserQueryService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User findUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public User findUserIfNullReturnNull(UserDetails userDetails){
        if(Objects.isNull(userDetails)){
            return null;
        }

        return userRepository.findByEmail(userDetails.getUsername()).orElse(null);
    }
}

