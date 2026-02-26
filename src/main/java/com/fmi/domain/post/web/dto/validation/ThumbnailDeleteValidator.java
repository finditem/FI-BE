package com.fmi.domain.post.web.dto.validation;

import com.fmi.domain.post.web.dto.request.PostUpdateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

public class ThumbnailDeleteValidator
        implements ConstraintValidator<ValidThumbnailDeletion, PostUpdateRequest> {

    @Override
    public boolean isValid(PostUpdateRequest request,
                           ConstraintValidatorContext context) {

        if (request == null) return true;

        List<Long> deleteList = request.deleteImageIdList();
        Long thumbnailId = request.thumbnailImageId();

        if (deleteList == null || deleteList.isEmpty()) return true;
        if (thumbnailId == null) return true;

        if (deleteList.contains(thumbnailId)) {

            context.disableDefaultConstraintViolation();

            context.buildConstraintViolationWithTemplate(
                            "썸네일 이미지는 삭제 목록에 포함될 수 없습니다."
                    )
                    .addPropertyNode("deleteImageIdList")
                    .addConstraintViolation();

            return false;
        }

        return true;
    }
}