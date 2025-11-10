package com.fmi.domain.user.web.dto.response;

import com.fmi.domain.Enum.Type;
import com.fmi.domain.user.data.UserCategory;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class UserCategoryResponse {

    Long id;
    Type category;
    LocalDateTime createdAt;

    public static UserCategoryResponse from(UserCategory category) {
        return UserCategoryResponse.builder()
                .id(category.getId())
                .category(category.getCategory())
                .createdAt(category.getCreatedAt())
                .build();
    }
}

