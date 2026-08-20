package com.fmi.domain.post.web.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidThumbnailKeepValidator.class)
@Documented
public @interface ValidThumbnailKeep {

    String message() default "thumbnailImageId는 keepImageIdList에 포함되어야 합니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
