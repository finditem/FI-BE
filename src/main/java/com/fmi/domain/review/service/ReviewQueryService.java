package com.fmi.domain.review.service;

import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.findcompletion.data.FindCompletion;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.post.service.PostImageService;
import com.fmi.domain.review.data.Review;
import com.fmi.domain.review.data.ReviewDetail;
import com.fmi.domain.review.data.ReviewListType;
import com.fmi.domain.review.data.ReviewPage;
import com.fmi.domain.review.data.ReviewPostInfo;
import com.fmi.domain.review.data.ReviewSummary;
import com.fmi.domain.review.data.ReviewUser;
import com.fmi.domain.review.exception.ReviewErrorStatus;
import com.fmi.domain.review.repository.ReviewRepository;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewQueryService {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final PostRepository postRepository;
    private final PostImageService postImageService;

    // 숨김 여부와 관계없이 조회한다. 숨김은 각자의 목록에서만 빼는 기능이다.
    public ReviewDetail getReview(Long reviewId) {
        Review review = reviewRepository
                .findDetailById(reviewId)
                .filter(Review::isActive)
                .orElseThrow(() -> new GeneralException(ReviewErrorStatus.NOT_FOUND));

        return new ReviewDetail(
                review.getId(),
                review.getEmotion(),
                review.getHelpTypes(),
                review.getContent(),
                review.getCreatedAt(),
                ReviewUser.from(review.getAuthor()),
                ReviewUser.from(review.getRecipient()),
                findPostInfo(review.getFindCompletion()));
    }

    // 마이페이지 후기 탭. 내가 숨긴 후기는 각 목록에서 제외한다.
    public ReviewPage getMyReviews(String userEmail, ReviewListType type, Long cursor, int size) {
        User user = findUser(userEmail);
        return switch (type) {
            case RECEIVED ->
                toPage(
                        reviewRepository.findReceivedReviews(user.getId(), true, cursor, size + 1),
                        size,
                        reviewRepository.countReceivedReviews(user.getId(), true));
            case SENT ->
                toPage(
                        reviewRepository.findSentReviews(user.getId(), cursor, size + 1),
                        size,
                        reviewRepository.countSentReviews(user.getId()));
        };
    }

    // 타인 프로필에서는 받은 사용자가 숨긴 후기도 보여주고, 총 개수는 계산하지 않는다.
    // 대상 사용자 존재 여부는 타인 페이지 조회에서 먼저 확인한다.
    public ReviewPage getProfileReceivedReviews(Long userId, Long cursor, int size) {
        List<ReviewSummary> rows = reviewRepository.findReceivedReviews(userId, false, cursor, size + 1);
        return toPage(rows, size, null);
    }

    // 게시글은 soft delete되어도 row가 남는다. 삭제 뒤 썸네일은 채팅방 상세와 같이 스냅샷을 쓴다.
    private ReviewPostInfo findPostInfo(FindCompletion findCompletion) {
        ChatRoom chatRoom = findCompletion.getChatRoom();
        String snapshotThumbnailUrl = chatRoom == null ? null : chatRoom.getSourceThumbnailUrl();

        return postRepository
                .findById(findCompletion.getSourcePostId())
                .map(post -> new ReviewPostInfo(
                        post.getId(),
                        post.getTitle(),
                        post.getAddress(),
                        post.isDeleted() ? snapshotThumbnailUrl : postImageService.findThumbnailImageUrl(post),
                        post.getDate(),
                        post.getCreatedAt(),
                        post.isDeleted()))
                .orElseGet(() -> new ReviewPostInfo(
                        findCompletion.getSourcePostId(),
                        chatRoom == null ? null : chatRoom.getSourceTitle(),
                        chatRoom == null ? null : chatRoom.getSourceAddress(),
                        snapshotThumbnailUrl,
                        null,
                        null,
                        true));
    }

    private User findUser(String userEmail) {
        return userRepository
                .findByEmail(userEmail)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
    }

    private ReviewPage toPage(List<ReviewSummary> rows, int size, Long totalCount) {
        boolean hasNext = rows.size() > size;
        List<ReviewSummary> reviews = hasNext ? rows.subList(0, size) : rows;
        Long nextCursor = hasNext ? reviews.get(reviews.size() - 1).reviewId() : null;
        List<Long> reviewIds = reviews.stream().map(ReviewSummary::reviewId).toList();

        return new ReviewPage(
                reviews, reviewRepository.findHelpTypesByReviewIds(reviewIds), totalCount, nextCursor, hasNext);
    }
}
