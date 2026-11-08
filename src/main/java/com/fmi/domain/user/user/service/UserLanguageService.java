package com.fmi.domain.user.user.service;

import com.fmi.domain.user.user.converter.UserConverter;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.web.dto.request.PreferredLanguageUpdateRequest;
import com.fmi.domain.user.user.web.dto.response.PreferredLanguageResponse;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserLanguageService {

    private final UserQueryService userQueryService;

    /**
     * 선호 언어 조회
     */
    @Transactional(readOnly = true)
    public PreferredLanguageResponse getPreferredLanguage(String email) {
        User user = userQueryService.findUser(email);

        return UserConverter.toPreferredLanguageResponse(user.getPreferredLanguage());
    }

    /**
     * 선호 언어 변경
     */
    @Transactional
    public PreferredLanguageResponse updatePreferredLanguage(String email, PreferredLanguageUpdateRequest request) {
        User user = userQueryService.findUser(email);

        user.changePreferredLanguage(request.getPreferredLanguage(), LocalDateTime.now());

        return UserConverter.toPreferredLanguageResponse(user.getPreferredLanguage());
    }
}
