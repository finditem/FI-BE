package com.fmi.domain.inquirycomment.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.inquiry.data.Inquiry;
import com.fmi.domain.inquiry.repository.InquiryRepository;
import com.fmi.domain.inquirycomment.converter.InquiryCommentConverter;
import com.fmi.domain.inquirycomment.data.InquiryComment;
import com.fmi.domain.inquirycomment.data.InquiryCommentImage;
import com.fmi.domain.inquirycomment.repository.InquiryCommentImageRepository;
import com.fmi.domain.inquirycomment.repository.InquiryCommentRepository;
import com.fmi.domain.inquirycomment.response.InquiryCommentResponse;
import com.fmi.domain.inquirycomment.response.InquiryCommentSliceResponse;
import com.fmi.domain.inquirycomment.web.dto.CreateInquiryCommentDto;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.service.S3Service;
import com.fmi.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InquiryCommentService {

    private final InquiryRepository inquiryRepository;
    private final InquiryCommentRepository inquiryCommentRepository;
    private final InquiryCommentImageRepository inquiryCommentImageRepository;
    private final UserRepository userRepository;
    private final InquiryCommentConverter inquiryCommentConverter;
    private final NotificationService notificationService;
    private final S3Service s3Service;
    private final EmailService emailService;

    /**
     * 댓글 작성
     */
    @Transactional
    public InquiryCommentResponse createComment(CreateInquiryCommentDto dto, List<MultipartFile> images,
                                                 UserDetails userDetails, Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._INQUIRY_NOT_FOUND));

        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        if (!canAccessInquiry(inquiry, userDetails)) {
            throw new GeneralException(ErrorStatus._INQUIRY_ACCESS_DENIED);
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        InquiryComment parent = null;
        if (dto.getParentId() != null) {
            parent = inquiryCommentRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_NOT_FOUND));
            if (!parent.getInquiry().getId().equals(inquiryId)) {
                throw new GeneralException(ErrorStatus._BAD_REQUEST);
            }
        }

        InquiryComment comment = inquiryCommentConverter.toCommentEntity(dto, user, inquiry, parent, null);
        InquiryComment savedComment = inquiryCommentRepository.save(comment);

        // 이미지 업로드
        List<InquiryCommentImage> savedImages = uploadImages(images, savedComment);

        // 마지막 댓글 작성자 기준으로 answered 자동 토글
        inquiry.updateAnswered(isAdmin(userDetails));

        try {
            sendCommentNotification(inquiry, savedComment, parent);
        } catch (Exception e) {
            log.warn("댓글 알림 발송 실패: {}", e.getMessage());
        }

        // 관리자 답변 시 문의 작성자에게 이메일 발송
        if (isAdmin(userDetails) && inquiry.getUser() != null) {
            try {
                String replyDate = java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
                        .format(java.time.LocalDateTime.now());
                String nickname = inquiry.getUser().getNickname() != null ? inquiry.getUser().getNickname() : "회원";

                emailService.sendHtmlEmailAsync(
                    inquiry.getUser().getEmail(),
                    "문의에 대한 답변이 도착했습니다",
                    "support-reply-email.html",
                    java.util.Map.of(
                        "name", nickname,
                        "TITLE", inquiry.getTitle(),
                        "DATE", replyDate,
                        "CONTENT", savedComment.getContent(),
                        "INQUIRY_ID", String.valueOf(inquiry.getId())
                    )
                );
            } catch (Exception e) {
                log.warn("문의 답변 이메일 발송 실패: inquiryId={}", inquiry.getId(), e);
            }
        }

        return toResponse(savedComment, userDetails, savedImages);
    }

    /**
     * 댓글 목록 조회 (커서 기반 무한스크롤)
     */
    @Transactional(readOnly = true)
    public InquiryCommentSliceResponse getComments(Long inquiryId, Long cursor, int size, UserDetails userDetails) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._INQUIRY_NOT_FOUND));

        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        if (!canAccessInquiry(inquiry, userDetails)) {
            throw new GeneralException(ErrorStatus._INQUIRY_ACCESS_DENIED);
        }

        Slice<InquiryComment> comments;
        if (cursor == null) {
            comments = inquiryCommentRepository.findTopByInquiryIdOrderByIdDesc(inquiryId, Pageable.ofSize(size));
        } else {
            comments = inquiryCommentRepository.findByInquiryIdAndIdLessThanOrderByIdDesc(inquiryId, cursor, Pageable.ofSize(size));
        }

        Long nextCursor = comments.hasNext()
                ? comments.getContent().get(comments.getContent().size() - 1).getId()
                : null;

        List<InquiryComment> commentList = comments.getContent();

        // 이미지 일괄 조회 (N+1 방지)
        List<Long> commentIds = commentList.stream().map(InquiryComment::getId).toList();
        Map<Long, List<InquiryCommentImage>> imageMap = buildImageMap(commentIds);

        List<InquiryCommentResponse> result = commentList.stream()
                .map(comment -> {
                    boolean canEdit = isOwner(comment, userDetails);
                    boolean canDelete = isOwner(comment, userDetails) || isAdmin(userDetails);
                    boolean admin = isCommentByAdmin(comment);
                    List<InquiryCommentImage> imgs = imageMap.getOrDefault(comment.getId(), List.of());
                    return inquiryCommentConverter.toCommentResponse(comment, canEdit, canDelete, admin, imgs);
                })
                .collect(Collectors.toList());

        return new InquiryCommentSliceResponse(result, comments.hasNext(), nextCursor);
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public InquiryCommentResponse updateComment(CreateInquiryCommentDto dto, List<MultipartFile> images,
                                                 UserDetails userDetails, Long commentId) {
        InquiryComment comment = inquiryCommentRepository.findById(commentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_NOT_FOUND));

        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }

        if (comment.getUser() == null || !comment.getUser().getEmail().equals(userDetails.getUsername())) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }

        comment.updateContent(dto.getContent());

        // 기존 이미지 삭제 후 새 이미지 업로드
        List<InquiryCommentImage> existingImages = inquiryCommentImageRepository.findByComment_Id(commentId);
        if (!existingImages.isEmpty()) {
            List<String> oldUrls = existingImages.stream().map(InquiryCommentImage::getImgUrl).toList();
            s3Service.delete(oldUrls);
            inquiryCommentImageRepository.deleteAllByCommentId(commentId);
        }

        List<InquiryCommentImage> savedImages = uploadImages(images, comment);

        return toResponse(comment, userDetails, savedImages);
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public InquiryCommentResponse deleteComment(UserDetails userDetails, Long commentId) {
        InquiryComment comment = inquiryCommentRepository.findById(commentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_NOT_FOUND));

        boolean isOwner = false;
        if (comment.getUser() != null && userDetails != null) {
            isOwner = comment.getUser().getEmail().equals(userDetails.getUsername());
        }

        boolean admin = isAdmin(userDetails);

        if (!isOwner && !admin) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }

        // 이미지 S3 삭제
        List<InquiryCommentImage> imgs = inquiryCommentImageRepository.findByComment_Id(commentId);
        if (!imgs.isEmpty()) {
            s3Service.delete(imgs.stream().map(InquiryCommentImage::getImgUrl).toList());
            inquiryCommentImageRepository.deleteAllByCommentId(commentId);
        }

        inquiryCommentRepository.delete(comment);

        return toResponse(comment, userDetails, List.of());
    }

    /**
     * 문의 상세 조회용 댓글 전체 반환
     */
    @Transactional(readOnly = true)
    public List<InquiryCommentResponse> getCommentsForDetail(Long inquiryId, UserDetails userDetails) {
        if (userDetails == null) {
            return java.util.Collections.emptyList();
        }
        List<InquiryComment> allComments = inquiryCommentRepository.findByInquiryIdOrderByIdAsc(inquiryId);

        // 이미지 일괄 조회
        List<Long> commentIds = allComments.stream().map(InquiryComment::getId).toList();
        Map<Long, List<InquiryCommentImage>> imageMap = buildImageMap(commentIds);

        return allComments.stream()
                .map(comment -> {
                    boolean canEdit = isOwner(comment, userDetails);
                    boolean canDelete = isOwner(comment, userDetails) || isAdmin(userDetails);
                    boolean admin = isCommentByAdmin(comment);
                    List<InquiryCommentImage> imgs = imageMap.getOrDefault(comment.getId(), List.of());
                    return inquiryCommentConverter.toCommentResponse(comment, canEdit, canDelete, admin, imgs);
                })
                .collect(Collectors.toList());
    }

    private boolean canAccessInquiry(Inquiry inquiry, UserDetails userDetails) {
        if (isAdmin(userDetails)) {
            return true;
        }
        if (inquiry.getUser() != null) {
            return inquiry.getUser().getEmail().equals(userDetails.getUsername());
        }
        return false;
    }

    private boolean isAdmin(UserDetails userDetails) {
        if (userDetails == null) return false;
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN"));
    }

    private boolean isCommentByAdmin(InquiryComment comment) {
        if (comment.getUser() == null) return false;
        return comment.getUser().getRole() != null
                && comment.getUser().getRole().name().equals("ADMIN");
    }

    private boolean isOwner(InquiryComment comment, UserDetails userDetails) {
        if (comment.getUser() != null && userDetails != null) {
            return comment.getUser().getEmail().equals(userDetails.getUsername());
        }
        return false;
    }

    private InquiryCommentResponse toResponse(InquiryComment comment, UserDetails userDetails, List<InquiryCommentImage> images) {
        boolean canEdit = isOwner(comment, userDetails);
        boolean canDelete = isOwner(comment, userDetails) || isAdmin(userDetails);
        boolean admin = isCommentByAdmin(comment);
        return inquiryCommentConverter.toCommentResponse(comment, canEdit, canDelete, admin, images);
    }

    private List<InquiryCommentImage> uploadImages(List<MultipartFile> images, InquiryComment comment) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        List<String> urls = s3Service.upload(images);
        List<InquiryCommentImage> commentImages = urls.stream()
                .map(url -> InquiryCommentImage.create(url, comment))
                .toList();
        return inquiryCommentImageRepository.saveAll(commentImages);
    }

    private Map<Long, List<InquiryCommentImage>> buildImageMap(List<Long> commentIds) {
        if (commentIds.isEmpty()) return Map.of();
        return inquiryCommentImageRepository.findByComment_IdIn(commentIds).stream()
                .collect(Collectors.groupingBy(img -> img.getComment().getId()));
    }

    private void sendCommentNotification(Inquiry inquiry, InquiryComment comment, InquiryComment parent) {
        if (parent != null && parent.getUser() != null && comment.getUser() != null) {
            if (!parent.getUser().getId().equals(comment.getUser().getId())) {
                notificationService.createNotification(
                        parent.getUser(),
                        NotificationType.COMMENT,
                        "문의하신 내용에 답변이 생겼어요.",
                        comment.getContent(),
                        ReferenceType.INQUIRY,
                        inquiry.getId()
                );
            }
        } else if (parent == null && inquiry.getUser() != null && comment.getUser() != null) {
            if (!inquiry.getUser().getId().equals(comment.getUser().getId())) {
                notificationService.createNotification(
                        inquiry.getUser(),
                        NotificationType.COMMENT,
                        "문의하신 내용에 답변이 생겼어요.",
                        comment.getContent(),
                        ReferenceType.INQUIRY,
                        inquiry.getId()
                );
            }
        }
    }
}
