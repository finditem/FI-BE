package com.fmi.domain.review.service.internal;

import com.fmi.domain.findcompletion.data.FindCompletion;
import com.fmi.domain.review.exception.ReviewErrorStatus;
import com.fmi.domain.review.repository.ReviewRepository;
import com.fmi.domain.user.data.User;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewValidator {

    private static final int MAX_CONTENT_LENGTH = 300;

    private final ReviewRepository reviewRepository;

    public void validateWritable(FindCompletion findCompletion, User author, String content) {
        if (!findCompletion.isRequestedBy(author.getId())) {
            throw new GeneralException(ReviewErrorStatus.ACCESS_DENIED);
        }
        if (reviewRepository.existsByFindCompletionId(findCompletion.getId())) {
            throw new GeneralException(ReviewErrorStatus.ALREADY_WRITTEN);
        }

        if (content != null && content.codePointCount(0, content.length()) > MAX_CONTENT_LENGTH) {
            throw new GeneralException(ReviewErrorStatus.INVALID_CONTENT);
        }
    }
}
