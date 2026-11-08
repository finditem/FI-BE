package com.fmi.domain.user.user.service;

import com.fmi.domain.inquiry.comment.data.InquiryComment;
import com.fmi.domain.inquiry.comment.repository.InquiryCommentRepository;
import com.fmi.domain.inquiry.inquiry.data.Inquiry;
import com.fmi.domain.inquiry.inquiry.repository.InquiryRepository;
import com.fmi.domain.post.comment.data.Comment;
import com.fmi.domain.post.comment.repository.CommentRepository;
import com.fmi.domain.post.comment.service.CommentImageService;
import com.fmi.domain.post.comment.service.CommentLikeService;
import com.fmi.domain.post.comment.web.dto.response.CommentImageResponse;
import com.fmi.domain.post.post.data.Post;
import com.fmi.domain.post.post.data.PostFavorite;
import com.fmi.domain.post.post.data.enums.Category;
import com.fmi.domain.post.post.data.enums.PostStatus;
import com.fmi.domain.post.post.data.enums.PostType;
import com.fmi.domain.post.post.repository.PostFavoriteRepository;
import com.fmi.domain.post.post.repository.PostRepository;
import com.fmi.domain.post.post.repository.PostSearchRepository;
import com.fmi.domain.post.post.web.dto.response.PostPageResponse;
import com.fmi.domain.report.data.Report;
import com.fmi.domain.report.repository.ReportRepository;
import com.fmi.domain.user.user.converter.UserConverter;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.data.enums.ActivityType;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.domain.user.user.web.dto.response.ActivityResponse;
import com.fmi.domain.user.user.web.dto.response.DailyActivityResponse;
import com.fmi.domain.user.user.web.dto.response.MyActivityPageResponse;
import com.fmi.domain.user.user.web.dto.response.MyCommentPageResponse;
import com.fmi.domain.user.user.web.dto.response.UserCommentSummaryResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.enums.SortType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserActivityQueryService {
    private final PostSearchRepository postSearchRepository;

    private final UserRepository userRepository;

    private final PostRepository postRepository;

    private final CommentRepository commentRepository;

    private final PostFavoriteRepository postFavoriteRepository;

    private final InquiryRepository inquiryRepository;

    private final ReportRepository reportRepository;

    private final CommentLikeService commentLikeService;

    private final CommentImageService commentImageService;

    private final InquiryCommentRepository inquiryCommentRepository;

    /**
     * 내가 쓴 댓글 목록 조회 (커서 기반 + 필터)
     */
    @Transactional(readOnly = true)
    public MyCommentPageResponse getMyComments(
            String email,
            SortType sortType,
            LocalDate startDate,
            LocalDate endDate,
            String keyword,
            Long cursor,
            int size) {
        User user =
                userRepository.findByEmail(email).orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.plusDays(1).atStartOfDay() : null;
        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        PageRequest pageRequest = PageRequest.of(0, size);
        Slice<Comment> commentSlice = (sortType == SortType.OLDEST)
                ? commentRepository.searchMyCommentsOldest(
                        user, cursor, startDateTime, endDateTime, trimmedKeyword, pageRequest)
                : commentRepository.searchMyCommentsLatest(
                        user, cursor, startDateTime, endDateTime, trimmedKeyword, pageRequest);

        List<Comment> commentList = commentSlice.getContent();
        List<Long> commentIds = commentList.stream().map(Comment::getId).toList();

        Map<Long, Long> likeCountMap = commentLikeService.buildLikeCountMap(commentIds);
        Set<Long> myLikeSet = commentLikeService.buildMyLikeSet(commentIds, user);
        Map<Long, List<CommentImageResponse>> imageMap = commentImageService.buildImageMap(commentIds);

        List<UserCommentSummaryResponse> comments = commentList.stream()
                .map(comment -> UserConverter.toUserCommentSummaryResponse(
                        comment,
                        likeCountMap.getOrDefault(comment.getId(), 0L),
                        myLikeSet.contains(comment.getId()),
                        imageMap.getOrDefault(comment.getId(), List.of())))
                .toList();

        Long nextCursor =
                commentSlice.hasNext() ? commentList.get(commentList.size() - 1).getId() : null;
        return new MyCommentPageResponse(comments, nextCursor, commentSlice.hasNext());
    }

    /**
     * 내가 쓴 게시글 목록 조회 (커서 기반 + 필터)
     */
    @Transactional(readOnly = true)
    public PostPageResponse getMyPosts(
            String email,
            PostType postType,
            PostStatus postStatus,
            Category category,
            SortType sortType,
            LocalDate startDate,
            LocalDate endDate,
            String keyword,
            Long cursor,
            int size) {
        User user =
                userRepository.findByEmail(email).orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        return postSearchRepository.searchMyPosts(
                user.getId(), postType, postStatus, category, sortType, startDate, endDate, keyword, cursor, size);
    }

    /**
     * 내 활동 내역 통합 조회 (커서 기반, createdAt 내림차순, 날짜별 그룹화)
     */
    @Transactional(readOnly = true)
    public MyActivityPageResponse getMyActivities(
            String email,
            ActivityType type,
            LocalDate startDate,
            LocalDate endDate,
            String keyword,
            String cursor,
            int size) {
        User user =
                userRepository.findByEmail(email).orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        size = Math.max(1, Math.min(size, 50));

        LocalDateTime cursorTime =
                (cursor != null) ? LocalDateTime.parse(cursor, DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;

        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.plusDays(1).atStartOfDay() : null;
        String trimmedKeyword =
                (keyword != null && !keyword.isBlank()) ? keyword.trim().toLowerCase() : null;

        PageRequest pageRequest = PageRequest.of(0, size);

        List<ActivityResponse> allActivities = new ArrayList<>();
        boolean anySliceHasNext = false;

        if (type == null || type == ActivityType.POST) {
            Slice<Post> postSlice = postRepository.findUserActivities(
                    user, startDateTime, endDateTime, trimmedKeyword, cursorTime, pageRequest);
            anySliceHasNext = anySliceHasNext || postSlice.hasNext();
            postSlice
                    .getContent()
                    .forEach(p -> allActivities.add(new ActivityResponse(
                            "POST", p.getId(), p.getTitle(), truncate(p.getContent(), 100), p.getCreatedAt())));
        }

        if (type == null || type == ActivityType.COMMENT) {
            Slice<Comment> commentSlice = commentRepository.findUserActivityComments(
                    user, startDateTime, endDateTime, trimmedKeyword, cursorTime, pageRequest);
            anySliceHasNext = anySliceHasNext || commentSlice.hasNext();
            commentSlice
                    .getContent()
                    .forEach(c -> allActivities.add(new ActivityResponse(
                            "COMMENT",
                            c.getId(),
                            c.getPost().getTitle(),
                            truncate(c.getContent(), 100),
                            c.getCreatedAt())));
        }

        if (type == null || type == ActivityType.FAVORITE) {
            Slice<PostFavorite> favoriteSlice = postFavoriteRepository.findUserActivityFavorites(
                    user, startDateTime, endDateTime, trimmedKeyword, cursorTime, pageRequest);
            anySliceHasNext = anySliceHasNext || favoriteSlice.hasNext();
            favoriteSlice
                    .getContent()
                    .forEach(f -> allActivities.add(new ActivityResponse(
                            "FAVORITE", f.getFavorite_id(), f.getPost().getTitle(), null, f.getCreatedAt())));
        }

        // 문의 등록 활동 (INQUIRY_RECEIVED)
        if (type == null || type == ActivityType.INQUIRY || type == ActivityType.INQUIRY_RECEIVED) {
            Slice<Inquiry> inquirySlice = inquiryRepository.findUserActivityInquiries(
                    user, startDateTime, endDateTime, trimmedKeyword, cursorTime, pageRequest);
            anySliceHasNext = anySliceHasNext || inquirySlice.hasNext();
            inquirySlice
                    .getContent()
                    .forEach(i -> allActivities.add(new ActivityResponse(
                            "INQUIRY_RECEIVED",
                            i.getId(),
                            i.getTitle(),
                            truncate(i.getContent(), 100),
                            i.getCreatedAt())));
        }

        // 문의 답변 등록 활동 (INQUIRY_ANSWERED) - InquiryComment 기반
        if (type == null || type == ActivityType.INQUIRY || type == ActivityType.INQUIRY_ANSWERED) {
            Slice<InquiryComment> commentSlice = inquiryCommentRepository.findUserActivityInquiryAnswers(
                    user, startDateTime, endDateTime, trimmedKeyword, cursorTime, pageRequest);
            anySliceHasNext = anySliceHasNext || commentSlice.hasNext();
            commentSlice
                    .getContent()
                    .forEach(c -> allActivities.add(new ActivityResponse(
                            "INQUIRY_ANSWERED",
                            c.getId(),
                            c.getInquiry().getTitle(),
                            truncate(c.getContent(), 100),
                            c.getCreatedAt())));
        }

        if (type == null
                || type == ActivityType.REPORT
                || type == ActivityType.REPORT_RECEIVED
                || type == ActivityType.REPORT_ANSWERED) {
            Slice<Report> reportSlice = reportRepository.findUserActivityReports(
                    user.getId(), startDateTime, endDateTime, trimmedKeyword, cursorTime, pageRequest);
            anySliceHasNext = anySliceHasNext || reportSlice.hasNext();
            reportSlice.getContent().stream()
                    .filter(r -> {
                        if (type == ActivityType.REPORT_RECEIVED) return !Boolean.TRUE.equals(r.getAnswered());
                        if (type == ActivityType.REPORT_ANSWERED) return Boolean.TRUE.equals(r.getAnswered());
                        return true;
                    })
                    .forEach(r -> {
                        boolean isAnswered = Boolean.TRUE.equals(r.getAnswered());
                        String activityType = isAnswered ? "REPORT_ANSWERED" : "REPORT_RECEIVED";
                        allActivities.add(new ActivityResponse(
                                activityType,
                                r.getReportId(),
                                r.getTargetType().getDescription() + " 신고",
                                truncate(r.getReason(), 100),
                                r.getCreatedAt()));
                    });
        }

        allActivities.sort(Comparator.comparing(ActivityResponse::createdAt).reversed());

        boolean hasNext = anySliceHasNext || allActivities.size() > size;
        List<ActivityResponse> result = allActivities.size() > size ? allActivities.subList(0, size) : allActivities;

        String nextCursorValue = (hasNext && !result.isEmpty())
                ? result.get(result.size() - 1).createdAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : null;

        List<DailyActivityResponse> grouped = result.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        a -> a.createdAt().toLocalDate().toString(),
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()))
                .entrySet()
                .stream()
                .map(e -> new DailyActivityResponse(e.getKey(), e.getValue()))
                .toList();

        return new MyActivityPageResponse(grouped, nextCursorValue, hasNext);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
