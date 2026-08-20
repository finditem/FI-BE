package com.fmi.domain.user.web.dto.response;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.user.data.UserCategory;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserCategoryResponse {

    Long id;
    Category category;
    LocalDateTime createdAt;

    public static UserCategoryResponse from(UserCategory category) {
        return UserCategoryResponse.builder()
                .id(category.getId())
                .category(category.getCategory())
                .createdAt(category.getCreatedAt())
                .build();
    }
}
