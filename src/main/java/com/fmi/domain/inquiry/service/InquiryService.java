package com.fmi.domain.inquiry.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.inquiry.converter.InquiryConverter;
import com.fmi.domain.inquiry.data.Inquiry;
import com.fmi.domain.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.data.enums.InquiryType;
import com.fmi.domain.inquiry.event.InquiryEvent;
import com.fmi.domain.inquiry.repository.InquiryRepository;
import com.fmi.domain.inquiry.web.dto.request.InquiryCreateRequestDTO;
import com.fmi.domain.inquiry.web.dto.response.InquiryDetailDTO;
import com.fmi.domain.inquiry.web.dto.response.InquiryListDTO;
import com.fmi.domain.inquirycomment.response.InquiryCommentResponse;
import com.fmi.domain.inquirycomment.service.InquiryCommentService;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {
    
    private final InquiryRepository inquiryRepository;
    private final InquiryConverter inquiryConverter;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final InquiryCommentService inquiryCommentService;

    /**
     * 1:1 개인 문의 생성
     */
    @Transactional
    public Long createInquiry(InquiryCreateRequestDTO request, UserDetails userDetails) {

        User user = null;
        String resolvedEmail;

        if (userDetails != null) {
            user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
            resolvedEmail = user.getEmail();
        } else {
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                throw new GeneralException(ErrorStatus._INQUIRY_GUEST_EMAIL_REQUIRED);
            }
            resolvedEmail = request.getEmail().trim();
        }

        Inquiry saved = inquiryRepository.save(
                Inquiry.builder()
                        .title(request.getTitle())
                        .content(request.getContent())
                        .category(request.getCategory())
                        .inquiryType(InquiryType.PRIVATE)
                        .user(user) // 회원이면 user 세팅
                        .email(user == null ? resolvedEmail : null) // 비회원이면 email 컬럼 세팅
                        .build()
        );

        eventPublisher.publishEvent(InquiryEvent.from(saved));
        
        // 문의 접수 이메일 발송
        try {
            String recipientEmail = saved.getEmail() != null ? saved.getEmail() : 
                                   (saved.getUser() != null ? saved.getUser().getEmail() : null);
            if (recipientEmail != null) {
                String inquiryDate = java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
                        .format(saved.getCreatedAt() != null ? saved.getCreatedAt() : java.time.LocalDateTime.now());
                
                emailService.sendHtmlEmail(
                    recipientEmail,
                    "문의가 접수되었습니다",
                    "support-request-email.html",
                    java.util.Map.of(
                        "TITLE", saved.getTitle(),
                        "DATE", inquiryDate,
                        "CONTENT", saved.getContent() != null ? saved.getContent() : ""
                    )
                );
            }
        } catch (Exception e) {
            // 이메일 발송 실패해도 문의는 성공 처리
        }
        
        return saved.getId();
    }

    /**
     * 내 문의 내역 조회
     */
    public Page<InquiryListDTO> getMyInquiries(User user, Pageable pageable) {
        Page<Inquiry> inquiries = inquiryRepository.findByUser(user, pageable);
        
        return inquiries.map(inquiry -> inquiryConverter.toListDTO(inquiry));
    }

    /**
     * 문의 상세 조회
     * - 회원 문의: user로 확인
     * - 관리자: 항상 접근 가능
     */
    public InquiryDetailDTO getInquiryDetail(Long inquiryId, UserDetails userDetails) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._INQUIRY_NOT_FOUND));

        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        
        // 권한 확인: 1:1 개인 문의는 본인만 조회 가능
        // 회원 문의인 경우
        if (inquiry.getUser() != null) {
            if (!inquiry.getUser().getId().equals(user.getId())) {
                throw new GeneralException(ErrorStatus._INQUIRY_ACCESS_DENIED);
            }
        } else if (!isAdmin(userDetails)) {
            throw new GeneralException(ErrorStatus._INQUIRY_ACCESS_DENIED);
        }
        
        java.util.List<InquiryCommentResponse> comments =
                inquiryCommentService.getCommentsForDetail(inquiry.getId(), userDetails);
        return inquiryConverter.toDetailDTO(inquiry, comments);
    }
    
    /**
     * 문의 상태 변경(관리자)
     */
    @Transactional
    public void updateStatus(Long inquiryId, InquiryStatus status) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._INQUIRY_NOT_FOUND));

        switch (status) {
            case RECEIVED:
                inquiry.markAsReceived();
                break;
            case PENDING:
                inquiry.markAsPending();
                break;
            case ANSWERED:
                inquiry.markAsAnswered();
                break;
            default:
                throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }

        // 문의자에게 상태 변경 알림 및 이메일 발송
        String statusMessage = getStatusMessage(status);
        
        // 회원 문의인 경우 알림 및 이메일 발송
        if (inquiry.getUser() != null) {
            notificationService.createNotification(
                    inquiry.getUser(),
                    NotificationType.INQUIRY_REPLY,
                    "문의 상태가 변경되었습니다",
                    statusMessage,
                    ReferenceType.INQUIRY,
                    inquiry.getId()
            );
            
            // 문의 상태 변경 이메일 발송
            try {
                String statusDate = java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
                        .format(java.time.LocalDateTime.now());
                
                emailService.sendHtmlEmail(
                    inquiry.getUser().getEmail(),
                    "문의 상태가 변경되었습니다",
                    "inquiry-status-change-email.html",
                    java.util.Map.of(
                        "TITLE", inquiry.getTitle(),
                        "STATUS", statusMessage,
                        "DATE", statusDate
                    )
                );
            } catch (Exception e) {
                // 이메일 발송 실패해도 알림은 성공 처리
            }
        } else if (inquiry.getEmail() != null) {
            // 비회원 문의인 경우 이메일로만 발송
            try {
                String statusDate = java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
                        .format(java.time.LocalDateTime.now());
                
                emailService.sendHtmlEmail(
                    inquiry.getEmail(),
                    "문의 상태가 변경되었습니다",
                    "inquiry-status-change-email.html",
                    java.util.Map.of(
                        "TITLE", inquiry.getTitle(),
                        "STATUS", statusMessage,
                        "DATE", statusDate
                    )
                );
            } catch (Exception e) {
                // 이메일 발송 실패는 무시
            }
        }
    }
    
    /**
     * 상태에 따른 메시지 반환
     */
    private String getStatusMessage(InquiryStatus status) {
        return switch (status) {
            case RECEIVED -> "접수";
            case PENDING -> "보류";
            case ANSWERED -> "답변완료";
        };
    }

    private boolean isAdmin(UserDetails userDetails) {
        if (userDetails == null) {
            return false;
        }
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN"));
    }

}

