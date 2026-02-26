package com.fmi.domain.post.web.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ThumbnailDeleteValidator.class)
@Documented
public @interface ValidThumbnailDeletion {

    String message() default "썸네일 이미지는 삭제 목록에 포함될 수 없습니다.";

    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}