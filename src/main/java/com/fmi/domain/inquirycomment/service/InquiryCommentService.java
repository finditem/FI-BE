package com.fmi.domain.inquirycomment.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.inquiry.data.Inquiry;
import com.fmi.domain.inquiry.repository.InquiryRepository;
import com.fmi.domain.inquirycomment.converter.InquiryCommentConverter;
import com.fmi.domain.inquirycomment.data.InquiryComment;
import com.fmi.domain.inquirycomment.repository.InquiryCommentRepository;
import com.fmi.domain.inquirycomment.response.InquiryCommentResponse;
import com.fmi.domain.inquirycomment.response.InquiryCommentSliceResponse;
import com.fmi.domain.inquirycomment.web.dto.CreateInquiryCommentDto;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InquiryCommentService {

    private final InquiryRepository inquiryRepository;
    private final InquiryCommentRepository inquiryCommentRepository;
    private final UserRepository userRepository;
    private final InquiryCommentConverter inquiryCommentConverter;
    private final NotificationService notificationService;

    /**
     * 댓글 작성
     * - 문의 작성자(user 또는 email 일치) 또는 관리자만 작성 가능
     * - 비회원인 경우 email로 식별
     */
    @Transactional
    public InquiryCommentResponse createComment(CreateInquiryCommentDto dto, UserDetails userDetails, Long inquiryId, String email) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._INQUIRY_NOT_FOUND));

        // 권한 확인: 문의 작성자 또는 관리자만 댓글 작성 가능
        if (!canAccessInquiry(inquiry, userDetails, email)) {
            throw new GeneralException(ErrorStatus._INQUIRY_ACCESS_DENIED);
        }

        User user = null;
        String commentEmail = null;

        if (userDetails != null) {
            user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        } else if (email != null && !email.isBlank()) {
            // 비회원인 경우 이메일 사용
            commentEmail = email;
        } else {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        // 부모 댓글 조회 (대댓글인 경우)
        InquiryComment parent = null;
        if (dto.getParentId() != null) {
            parent = inquiryCommentRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_NOT_FOUND));

            // 부모 댓글이 같은 문의에 속하는지 확인
            if (!parent.getInquiry().getId().equals(inquiryId)) {
                throw new GeneralException(ErrorStatus._BAD_REQUEST);
            }
        }

        InquiryComment comment = inquiryCommentConverter.toCommentEntity(dto, user, inquiry, parent, commentEmail);
        InquiryComment savedComment = inquiryCommentRepository.save(comment);

        // 알림 발송 (선택적)
        try {
            sendCommentNotification(inquiry, savedComment, parent);
        } catch (Exception e) {
            log.warn("댓글 알림 발송 실패: {}", e.getMessage());
        }

        return toResponse(savedComment, userDetails, email);
    }

    /**
     * 댓글 목록 조회 (커서 기반 무한스크롤)
     * - 문의 작성자 또는 관리자만 조회 가능
     */
    @Transactional(readOnly = true)
    public InquiryCommentSliceResponse getComments(Long inquiryId, Long cursor, int size, UserDetails userDetails, String email) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._INQUIRY_NOT_FOUND));

        // 권한 확인: 문의 작성자 또는 관리자만 조회 가능
        if (!canAccessInquiry(inquiry, userDetails, email)) {
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

        List<InquiryCommentResponse> result = comments.getContent().stream()
                .map(comment -> toResponse(comment, userDetails, email))
                .collect(Collectors.toList());

        return new InquiryCommentSliceResponse(result, comments.hasNext(), nextCursor);
    }

    /**
     * 댓글 수정
     * - 작성자만 수정 가능 (비회원 댓글은 수정 불가)
     */
    @Transactional
    public InquiryCommentResponse updateComment(CreateInquiryCommentDto dto, UserDetails userDetails, Long commentId, String email) {
        InquiryComment comment = inquiryCommentRepository.findById(commentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_NOT_FOUND));

        // 비회원 댓글은 수정 불가
        if (comment.getUser() == null) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }

        // 작성자 확인
        if (userDetails == null || !comment.getUser().getEmail().equals(userDetails.getUsername())) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }

        comment.updateContent(dto.getContent());

        return toResponse(comment, userDetails, email);
    }

    /**
     * 댓글 삭제
     * - 작성자 또는 관리자만 삭제 가능 (관리자는 다른 사람의 댓글도 삭제 가능)
     */
    @Transactional
    public InquiryCommentResponse deleteComment(UserDetails userDetails, Long commentId, String email) {
        InquiryComment comment = inquiryCommentRepository.findById(commentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_NOT_FOUND));

        // 작성자 확인
        boolean isOwner = false;
        if (comment.getUser() != null && userDetails != null) {
            isOwner = comment.getUser().getEmail().equals(userDetails.getUsername());
        } else if (comment.getUser() == null && comment.getEmail() != null && email != null) {
            // 비회원 댓글인 경우 이메일로 확인
            isOwner = comment.getEmail().equals(email);
        }

        // 관리자 확인
        boolean isAdmin = isAdmin(userDetails);

        if (!isOwner && !isAdmin) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }

        inquiryCommentRepository.delete(comment);

        return toResponse(comment, userDetails, email);
    }

    /**
     * 문의 접근 권한 확인
     * - 문의 작성자(user 또는 email 일치) 또는 관리자만 접근 가능
     */
    private boolean canAccessInquiry(Inquiry inquiry, UserDetails userDetails, String email) {
        // 관리자는 항상 접근 가능
        if (isAdmin(userDetails)) {
            return true;
        }

        // 회원 문의인 경우
        if (inquiry.getUser() != null) {
            if (userDetails == null) {
                return false;
            }
            return inquiry.getUser().getEmail().equals(userDetails.getUsername());
        }

        // 비회원 문의인 경우 이메일로 확인
        if (inquiry.getEmail() != null && email != null) {
            return inquiry.getEmail().equals(email);
        }

        return false;
    }

    /**
     * 관리자 여부 확인
     */
    private boolean isAdmin(UserDetails userDetails) {
        if (userDetails == null) {
            return false;
        }

        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN"));
    }

    /**
     * 댓글 작성자 확인
     */
    private boolean isOwner(InquiryComment comment, UserDetails userDetails, String email) {
        // 회원 댓글인 경우
        if (comment.getUser() != null) {
            if (userDetails == null) {
                return false;
            }
            return comment.getUser().getEmail().equals(userDetails.getUsername());
        }

        // 비회원 댓글인 경우 이메일로 확인
        if (comment.getEmail() != null && email != null) {
            return comment.getEmail().equals(email);
        }

        return false;
    }

    /**
     * 엔티티를 Response로 변환
     */
    private InquiryCommentResponse toResponse(InquiryComment comment, UserDetails userDetails, String email) {
        boolean canEdit = isOwner(comment, userDetails, email) && comment.getUser() != null; // 비회원 댓글은 수정 불가
        boolean canDelete = isOwner(comment, userDetails, email) || isAdmin(userDetails);

        // Converter로 기본 Response 생성
        InquiryCommentResponse response = inquiryCommentConverter.toCommentResponse(comment, canEdit, canDelete);

        // 대댓글 목록 변환 (각 대댓글마다 권한 체크)
        List<InquiryCommentResponse> replies = comment.getReplies().stream()
                .map(reply -> {
                    boolean replyCanEdit = isOwner(reply, userDetails, email) && reply.getUser() != null;
                    boolean replyCanDelete = isOwner(reply, userDetails, email) || isAdmin(userDetails);
                    return inquiryCommentConverter.toCommentResponse(reply, replyCanEdit, replyCanDelete);
                })
                .collect(Collectors.toList());

        // 대댓글 목록 설정
        response.setReplies(replies);

        return response;
    }

    /**
     * 댓글 작성 시 알림 발송 (선택적)
     */
    private void sendCommentNotification(Inquiry inquiry, InquiryComment comment, InquiryComment parent) {
        // 대댓글인 경우 부모 댓글 작성자에게 알림
        if (parent != null && parent.getUser() != null && comment.getUser() != null) {
            // 자신의 댓글에 대댓글을 단 경우 알림 생략
            if (!parent.getUser().getId().equals(comment.getUser().getId())) {
                notificationService.createNotification(
                        parent.getUser(),
                        NotificationType.REPLY,
                        "댓글에 답글이 달렸습니다",
                        comment.getContent(),
                        ReferenceType.INQUIRY,
                        inquiry.getId()
                );
            }
        } else if (parent == null && inquiry.getUser() != null && comment.getUser() != null) {
            // 일반 댓글인 경우 문의 작성자에게 알림 (자신의 문의에 댓글을 단 경우 알림 생략)
            if (!inquiry.getUser().getId().equals(comment.getUser().getId())) {
                notificationService.createNotification(
                        inquiry.getUser(),
                        NotificationType.COMMENT,
                        "문의에 댓글이 달렸습니다",
                        comment.getContent(),
                        ReferenceType.INQUIRY,
                        inquiry.getId()
                );
            }
        }
    }
}
