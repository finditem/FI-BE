package com.fmi.domain.user.service;

import com.fmi.domain.Enum.Type;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.user.data.UserKeyword;
import com.fmi.domain.user.repository.UserKeywordRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserKeywordService {

    private final UserRepository userRepository;
    private final UserKeywordRepository userKeywordRepository;

    public List<UserKeyword> list(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        return userKeywordRepository.findAllByUser(user);
    }

    @Transactional
    public void add(Long userId, Type category, String keyword) {
        User user = userRepository.findById(userId).orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        String normalized = keyword.trim();
        if (normalized.isEmpty()) throw new GeneralException(ErrorStatus._BAD_REQUEST);
        if (userKeywordRepository.existsByUserAndCategoryAndKeyword(user, category, normalized)) return; // idempotent
        userKeywordRepository.save(UserKeyword.builder().user(user).category(category).keyword(normalized).build());
    }

    @Transactional
    public void remove(Long userId, Type category, String keyword) {
        User user = userRepository.findById(userId).orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        userKeywordRepository.deleteByUserAndCategoryAndKeyword(user, category, keyword.trim());
    }
}


