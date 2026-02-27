package com.fmi.domain.post.web.dto.validation;

import com.fmi.domain.post.web.dto.request.PostUpdateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

public class ValidThumbnailKeepValidator implements ConstraintValidator<ValidThumbnailKeep, PostUpdateRequest> {

    @Override
    public boolean isValid(PostUpdateRequest req, ConstraintValidatorContext context) {
        if (req == null) return true;

        Long thumbId = req.thumbnailImageId();
        List<Long> keepIds = req.keepImageIdList();

        if (thumbId == null) return true;

        if (keepIds == null || keepIds.isEmpty() || !keepIds.contains(thumbId)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "thumbnailImageId가 keepImageIdList에 없습니다."
            ).addPropertyNode("thumbnailImageId").addConstraintViolation();
            return false;
        }

        return true;
    }
}
