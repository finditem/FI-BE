package com.fmi.domain.review.service;

import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.findcompletion.data.FindCompletion;
import com.fmi.domain.findcompletion.exception.FindCompletionErrorStatus;
import com.fmi.domain.findcompletion.repository.FindCompletionRepository;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.review.data.Review;
import com.fmi.domain.review.data.ReviewEmotion;
import com.fmi.domain.review.data.ReviewHelpType;
import com.fmi.domain.review.exception.ReviewErrorStatus;
import com.fmi.domain.review.repository.ReviewRepository;
import com.fmi.domain.review.service.internal.ReviewValidator;
import com.fmi.domain.temperature.data.TemperatureActivityType;
import com.fmi.domain.temperature.service.TemperatureService;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final UserRepository userRepository;
    private final FindCompletionRepository findCompletionRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewValidator reviewValidator;
    private final TemperatureService temperatureService;
    private final NotificationService notificationService;
    private final PostRepository postRepository;

    @Transactional
    public Review write(
            Long findCompletionId,
            String userEmail,
            ReviewEmotion emotion,
            Set<ReviewHelpType> helpTypes,
            String content) {
        User author = userRepository
                .findByEmail(userEmail)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        FindCompletion findCompletion = findCompletionRepository
                .findById(findCompletionId)
                .orElseThrow(() -> new GeneralException(FindCompletionErrorStatus.NOT_FOUND));

        String normalizedContent = normalize(content);
        reviewValidator.validateWritable(findCompletion, author, normalizedContent);

        Review review = Review.builder()
                .findCompletion(findCompletion)
                .emotion(emotion)
                .helpTypes(helpTypes)
                .content(normalizedContent)
                .build();
        save(review);
        temperatureService.record(
                TemperatureActivityType.REVIEW_RECEIVED,
                review.getRecipient().getId(),
                review.getId(),
                review.getCreatedAt());
        notificationService.createNotification(
                review.getRecipient(),
                NotificationType.REVIEW,
                "새로운 후기가 도착했어요.",
                findPostTitle(findCompletion),
                ReferenceType.REVIEW,
                review.getId());
        return review;
    }

    @Transactional
    public void hide(Long reviewId, String userEmail) {
        User user = userRepository
                .findByEmail(userEmail)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        Review review = reviewRepository
                .findById(reviewId)
                .filter(Review::isActive)
                .orElseThrow(() -> new GeneralException(ReviewErrorStatus.NOT_FOUND));

        if (review.isWrittenBy(user.getId())) {
            review.hideByAuthor();
            return;
        }
        if (review.isReceivedBy(user.getId())) {
            review.hideByRecipient();
            return;
        }
        throw new GeneralException(ReviewErrorStatus.ACCESS_DENIED);
    }

    private String findPostTitle(FindCompletion findCompletion) {
        ChatRoom chatRoom = findCompletion.getChatRoom();
        return postRepository
                .findById(findCompletion.getSourcePostId())
                .map(Post::getTitle)
                .orElseGet(() -> chatRoom == null ? null : chatRoom.getSourceTitle());
    }

    private String normalize(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        return content.trim();
    }

    private void save(Review review) {
        try {
            reviewRepository.saveAndFlush(review);
        } catch (DataIntegrityViolationException e) {
            // 같은 찾기 완료 건에 동시에 작성하면 uk_review_completion에서 막힌다.
            throw new GeneralException(ReviewErrorStatus.ALREADY_WRITTEN);
        }
    }
}
