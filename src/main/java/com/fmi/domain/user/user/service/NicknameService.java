package com.fmi.domain.user.user.service;

import com.fmi.domain.user.user.service.internal.NicknameValidator;
import com.fmi.domain.user.user.web.dto.response.CheckResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NicknameService {

    private final NicknameValidator nicknameValidator;

    public CheckResponse check(String nickname) {
        nicknameValidator.validateAvailable(nickname);
        return new CheckResponse(true);
    }
}
