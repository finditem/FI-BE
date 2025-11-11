package com.fmi.domain.user.service;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.user.data.UserCategory;
import com.fmi.domain.user.repository.UserCategoryRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserCategoryService {

    private final UserRepository userRepository;
    private final UserCategoryRepository userCategoryRepository;

    public List<UserCategory> list(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        return userCategoryRepository.findAllByUser(user);
    }

    @Transactional
    public void add(String email, Category category) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        if (userCategoryRepository.existsByUserAndCategory(user, category)) {
            return; // idempotent
        }
        userCategoryRepository.save(UserCategory.builder()
                .user(user)
                .category(category)
                .build());
    }

    @Transactional
    public void remove(String email, Category category) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        userCategoryRepository.deleteByUserAndCategory(user, category);
    }
}
