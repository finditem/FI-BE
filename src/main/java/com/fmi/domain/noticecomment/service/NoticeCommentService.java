package com.fmi.domain.noticecomment.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.notice.data.Notice;
import com.fmi.domain.notice.repository.NoticeRepository;
import com.fmi.domain.noticecomment.converter.NoticeCommentConverter;
import com.fmi.domain.noticecomment.data.NoticeComment;
import com.fmi.domain.noticecomment.data.NoticeCommentImage;
import com.fmi.domain.noticecomment.repository.NoticeCommentImageRepository;
import com.fmi.domain.noticecomment.repository.NoticeCommentRepository;
import com.fmi.domain.noticecomment.response.NoticeCommentPageResponse;
import com.fmi.domain.noticecomment.response.NoticeCommentResponse;
import com.fmi.domain.noticecomment.response.NoticeCommentSliceResponse;
import com.fmi.domain.noticecomment.web.dto.CreateNoticeCommentDto;
import com.fmi.domain.noticecommentlike.data.NoticeCommentLike;
import com.fmi.domain.noticecommentlike.repository.NoticeCommentLikeRepository;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeCommentService {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final NoticeCommentRepository noticeCommentRepository;
    private final NoticeCommentConverter noticeCommentConverter;
    private final NoticeCommentLikeRepository noticeCommentLikeRepository;
    private final NoticeCommentImageRepository noticeCommentImageRepository;
    private final S3Service s3Service;
    private final NotificationService notificationService;

    @Transactional
    public NoticeCommentResponse createComment(Long noticeId, CreateNoticeCommentDto dto,
                                                List<MultipartFile> images, UserDetails userDetails) {
        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOTICE_NOT_FOUND));

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        NoticeComment parent = null;
        if (dto.getParentId() != null) {
            parent = noticeCommentRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_NOT_FOUND));
            if (!parent.getNotice().getNoticeId().equals(noticeId)) {
                throw new GeneralException(ErrorStatus._BAD_REQUEST);
            }
        }

        NoticeComment comment = noticeCommentConverter.toEntity(dto, user, notice, parent);
        NoticeComment saved = noticeCommentRepository.save(comment);

        // 이미지 업로드
        List<NoticeCommentImage> savedImages = uploadImages(images, saved);

        // 공지 작성자(관리자)에게 알림 (본인 댓글 제외)
        if (notice.getAuthor() != null && !notice.getAuthor().getId().equals(user.getId())) {
            notificationService.createNotification(
                    notice.getAuthor(),
                    NotificationType.COMMENT,
                    "새로운 댓글이 생겼어요.",
                    dto.getContent(),
                    ReferenceType.NOTICE,
                    notice.getNoticeId()
            );
        }

        // 대댓글인 경우 부모 댓글 작성자에게 알림 (본인 대댓글 제외)
        if (parent != null && parent.getUser() != null && !parent.getUser().getId().equals(user.getId())) {
            notificationService.createNotification(
                    parent.getUser(),
                    NotificationType.COMMENT,
                    "새로운 댓글이 생겼어요.",
                    dto.getContent(),
                    ReferenceType.NOTICE,
                    notice.getNoticeId()
            );
        }

        return toResponse(saved, userDetails, savedImages);
    }

    public NoticeCommentSliceResponse getComments(Long noticeId, Long cursor, int size, UserDetails userDetails) {
        if (!noticeRepository.existsById(noticeId)) {
            throw new GeneralException(ErrorStatus._NOTICE_NOT_FOUND);
        }

        Slice<NoticeComment> comments;
        if (cursor == null) {
            comments = noticeCommentRepository.findTopByNoticeIdOrderByIdDesc(noticeId, Pageable.ofSize(size));
        } else {
            comments = noticeCommentRepository.findByNoticeIdAndIdLessThanOrderByIdDesc(noticeId, cursor, Pageable.ofSize(size));
        }

        Long nextCursor = comments.hasNext()
                ? comments.getContent().get(comments.getContent().size() - 1).getId()
                : null;

        List<NoticeComment> commentList = comments.getContent().stream().limit(size).toList();

        // 이미지 일괄 조회 (N+1 방지)
        List<Long> commentIds = commentList.stream().map(NoticeComment::getId).toList();
        Map<Long, List<NoticeCommentImage>> imageMap = Map.of();
        if (!commentIds.isEmpty()) {
            imageMap = noticeCommentImageRepository.findByComment_IdIn(commentIds).stream()
                    .collect(Collectors.groupingBy(img -> img.getComment().getId()));
        }

        // 좋아요 상태 일괄 조회
        Set<Long> likedCommentIds = buildLikedCommentIds(commentIds, userDetails);

        // 대댓글 수 일괄 조회
        Map<Long, Long> childCountMap = buildChildCountMap(commentIds);

        Map<Long, List<NoticeCommentImage>> finalImageMap = imageMap;
        List<NoticeCommentResponse> result = commentList.stream()
                .map(comment -> {
                    boolean isOwner = isOwner(comment, userDetails);
                    boolean isAdmin = isAdmin(userDetails);
                    boolean isLike = likedCommentIds.contains(comment.getId());
                    long childCount = childCountMap.getOrDefault(comment.getId(), 0L);
                    List<NoticeCommentImage> imgs = finalImageMap.getOrDefault(comment.getId(), List.of());
                    return noticeCommentConverter.toResponse(comment, isOwner, isOwner || isAdmin, isLike, childCount, imgs);
                })
                .toList();

        return new NoticeCommentSliceResponse(result, comments.hasNext(), nextCursor);
    }

    private static final int REPLY_PAGE_SIZE = 10;

    public NoticeCommentPageResponse getReplies(Long parentId, int page, UserDetails userDetails) {
        NoticeComment parent = noticeCommentRepository.findById(parentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, REPLY_PAGE_SIZE);
        org.springframework.data.domain.Page<NoticeComment> repliesPage = noticeCommentRepository.findRepliesByParentId(parentId, pageable);
        long totalCount = repliesPage.getTotalElements();

        boolean hasNext = repliesPage.hasNext();
        Integer nextPage = hasNext ? page + 1 : null;
        long shown = (long) page * REPLY_PAGE_SIZE + repliesPage.getContent().size();
        long remainingCount = Math.max(0, Math.min(REPLY_PAGE_SIZE, totalCount - shown));

        List<NoticeComment> replyList = repliesPage.getContent();
        List<Long> replyIds = replyList.stream().map(NoticeComment::getId).toList();

        Map<Long, List<NoticeCommentImage>> imageMap = Map.of();
        if (!replyIds.isEmpty()) {
            imageMap = noticeCommentImageRepository.findByComment_IdIn(replyIds).stream()
                    .collect(Collectors.groupingBy(img -> img.getComment().getId()));
        }

        Set<Long> likedCommentIds = buildLikedCommentIds(replyIds, userDetails);
        Map<Long, Long> childCountMap = buildChildCountMap(replyIds);

        Map<Long, List<NoticeCommentImage>> finalImageMap = imageMap;
        List<NoticeCommentResponse> result = replyList.stream()
                .map(reply -> {
                    boolean isOwner = isOwner(reply, userDetails);
                    boolean isAdmin = isAdmin(userDetails);
                    boolean isLike = likedCommentIds.contains(reply.getId());
                    long childCount = childCountMap.getOrDefault(reply.getId(), 0L);
                    List<NoticeCommentImage> imgs = finalImageMap.getOrDefault(reply.getId(), List.of());
                    return noticeCommentConverter.toResponse(reply, isOwner, isOwner || isAdmin, isLike, childCount, imgs);
                })
                .toList();

        return new NoticeCommentPageResponse(result, hasNext, nextPage, totalCount, remainingCount);
    }

    @Transactional
    public NoticeCommentResponse updateComment(Long commentId, CreateNoticeCommentDto dto,
                                                List<MultipartFile> images, UserDetails userDetails) {
        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        NoticeComment comment = noticeCommentRepository.findById(commentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_NOT_FOUND));

        if (!comment.getUser().getEmail().equals(userDetails.getUsername())) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }

        comment.updateContent(dto.getContent());

        // 기존 이미지 삭제 후 새 이미지 업로드
        List<NoticeCommentImage> existingImages = noticeCommentImageRepository.findByComment_Id(commentId);
        if (!existingImages.isEmpty()) {
            List<String> oldUrls = existingImages.stream().map(NoticeCommentImage::getImgUrl).toList();
            s3Service.delete(oldUrls);
            noticeCommentImageRepository.deleteAllByCommentId(commentId);
        }

        List<NoticeCommentImage> savedImages = uploadImages(images, comment);

        return toResponse(comment, userDetails, savedImages);
    }

    @Transactional
    public void deleteComment(Long commentId, UserDetails userDetails) {
        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        NoticeComment comment = noticeCommentRepository.findById(commentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_NOT_FOUND));

        boolean isOwner = comment.getUser().getEmail().equals(userDetails.getUsername());
        boolean isAdmin = isAdmin(userDetails);

        if (!isOwner && !isAdmin) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }

        // 이미지 S3 삭제
        List<NoticeCommentImage> images = noticeCommentImageRepository.findByComment_Id(commentId);
        if (!images.isEmpty()) {
            s3Service.delete(images.stream().map(NoticeCommentImage::getImgUrl).toList());
            noticeCommentImageRepository.deleteAllByCommentId(commentId);
        }

        // 대댓글이 있으면 소프트 삭제, 없으면 하드 삭제
        if (!comment.getReplies().isEmpty()) {
            comment.softDelete();
        } else {
            noticeCommentRepository.delete(comment);
        }
    }

    @Transactional
    public void addCommentLike(Long commentId, String email) {
        NoticeComment comment = noticeCommentRepository.findById(commentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_NOT_FOUND));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        if (noticeCommentLikeRepository.findByUserAndComment(user, comment).isPresent()) {
            return;
        }

        noticeCommentLikeRepository.save(NoticeCommentLike.builder()
                .user(user)
                .comment(comment)
                .build());
        noticeCommentRepository.incrementLikeCount(commentId);
    }

    @Transactional
    public void removeCommentLike(Long commentId, String email) {
        NoticeComment comment = noticeCommentRepository.findById(commentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_NOT_FOUND));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        noticeCommentLikeRepository.findByUserAndComment(user, comment).ifPresent(like -> {
            noticeCommentLikeRepository.delete(like);
            noticeCommentRepository.decrementLikeCount(commentId);
        });
    }

    private NoticeCommentResponse toResponse(NoticeComment comment, UserDetails userDetails,
                                              List<NoticeCommentImage> images) {
        boolean isOwner = isOwner(comment, userDetails);
        boolean isAdmin = isAdmin(userDetails);
        long childCount = noticeCommentRepository.countByParentId(comment.getId());
        boolean isLike = checkIsLike(comment.getId(), userDetails);
        return noticeCommentConverter.toResponse(comment, isOwner, isOwner || isAdmin, isLike, childCount, images);
    }

    private List<NoticeCommentImage> uploadImages(List<MultipartFile> images, NoticeComment comment) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        List<String> urls = s3Service.upload(images);
        List<NoticeCommentImage> commentImages = urls.stream()
                .map(url -> NoticeCommentImage.create(url, comment))
                .toList();
        return noticeCommentImageRepository.saveAll(commentImages);
    }

    private boolean isOwner(NoticeComment comment, UserDetails userDetails) {
        return userDetails != null
                && comment.getUser() != null
                && comment.getUser().getEmail().equals(userDetails.getUsername());
    }

    private boolean isAdmin(UserDetails userDetails) {
        if (userDetails == null) return false;
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN"));
    }

    private boolean checkIsLike(Long commentId, UserDetails userDetails) {
        if (userDetails == null) return false;
        return userRepository.findByEmail(userDetails.getUsername())
                .map(user -> noticeCommentLikeRepository.findByUserAndComment(user,
                        noticeCommentRepository.findById(commentId).orElse(null)).isPresent())
                .orElse(false);
    }

    private Set<Long> buildLikedCommentIds(List<Long> commentIds, UserDetails userDetails) {
        if (userDetails == null || commentIds.isEmpty()) return Set.of();
        return userRepository.findByEmail(userDetails.getUsername())
                .map(user -> noticeCommentLikeRepository.findByUserIdAndCommentIdIn(user.getId(), commentIds))
                .orElse(Set.of());
    }

    private Map<Long, Long> buildChildCountMap(List<Long> parentIds) {
        if (parentIds.isEmpty()) return Map.of();
        return noticeCommentRepository.countByParentIds(parentIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}
