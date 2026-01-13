package com.fmi.domain.inquiry.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.inquiry.converter.InquiryConverter;
import com.fmi.domain.inquiry.data.InquiryReply;
import com.fmi.domain.inquiry.data.enums.InquiryCategory;
import com.fmi.domain.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.data.enums.InquiryType;
import com.fmi.domain.inquiry.repository.InquiryReplyRepository;
import com.fmi.domain.inquiry.repository.InquiryRepository;
import com.fmi.domain.inquiry.web.dto.request.InquiryPrivateRequestDTO;
import com.fmi.domain.inquiry.web.dto.request.InquiryPublicRequestDTO;
import com.fmi.domain.inquiry.web.dto.request.InquiryCreateRequestDTO;
import com.fmi.domain.inquiry.web.dto.response.InquiryDetailDTO;
import com.fmi.domain.inquiry.web.dto.response.InquiryListDTO;
import com.fmi.domain.notification.data.enums.ReferenceType;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fmi.domain.inquiry.data.Inquiry;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.service.EmailService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {
    
    private final InquiryRepository inquiryRepository;
    private final InquiryReplyRepository inquiryReplyRepository;
    private final InquiryConverter inquiryConverter;
    private final NotificationService notificationService;
    private final EmailService emailService;
    
    /**
     * 공개 문의 작성
     */
    @Transactional
    public Long createPublicInquiry(InquiryPublicRequestDTO request, User user) {
        Inquiry inquiry = Inquiry.builder()
                .user(user)  // 비회원인 경우 null
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .inquiryType(InquiryType.PUBLIC)
                .email(request.getEmail())  // 비회원용 이메일
                .build();
        
        Inquiry saved = inquiryRepository.save(inquiry);
        return saved.getId();
    }
    
    /**
     * 1:1 개인 문의 작성
     */
    @Transactional
    public Long createPrivateInquiry(InquiryPrivateRequestDTO request, User user) {
        Inquiry inquiry = Inquiry.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .inquiryType(InquiryType.PRIVATE)
                .build();
        
        Inquiry saved = inquiryRepository.save(inquiry);
        return saved.getId();
    }

    /**
     * 단일 엔드포인트: 문의 생성 (PUBLIC/PRIVATE)
     */
    @Transactional
    public Long createInquiry(InquiryCreateRequestDTO request, User user) {
        InquiryType type = request.getInquiryType();
        if (type == null) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }
        Inquiry.InquiryBuilder builder = Inquiry.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .inquiryType(type);

        if (type == InquiryType.PUBLIC) {
            // 공개 문의: 회원만 가능 + email 필수
            if (user == null) {
                throw new GeneralException(ErrorStatus._INQUIRY_ACCESS_DENIED);
            }
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                throw new GeneralException(ErrorStatus._BAD_REQUEST);
            }
            builder.user(user).email(request.getEmail());
        } else {
            // 1:1 문의: 비회원도 가능 (email은 정책에 따라 선택)
            builder.user(user);
            if (user == null && request.getEmail() != null && !request.getEmail().isBlank()) {
                builder.email(request.getEmail());
            }
        }

        Inquiry saved = inquiryRepository.save(builder.build());
        
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
     * 문의 답변 생성(관리자)
     */
    @Transactional
    public Long addReply(Long inquiryId, String content, Long adminId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._INQUIRY_NOT_FOUND));

        InquiryReply reply = InquiryReply.builder()
                .inquiry(inquiry)
                .adminId(adminId)
                .content(content)
                .build();

        InquiryReply saved = inquiryReplyRepository.save(reply);

        // 회원 문의인 경우에만 알림 및 이메일 발송
        if (inquiry.getUser() != null) {
            notificationService.createNotification(
                    inquiry.getUser(),
                    NotificationType.INQUIRY_REPLY,
                    "문의에 답변이 등록되었습니다",
                    content,
                    ReferenceType.INQUIRY,
                    inquiry.getId()
            );
            
            // 문의 답변 이메일 발송
            try {
                String replyDate = java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
                        .format(java.time.LocalDateTime.now());
                
                emailService.sendHtmlEmail(
                    inquiry.getUser().getEmail(),
                    "문의에 답변이 등록되었습니다",
                    "support-reply-email.html",
                    java.util.Map.of(
                        "TITLE", inquiry.getTitle(),
                        "DATE", replyDate,
                        "CONTENT", content
                    )
                );
            } catch (Exception e) {
                // 이메일 발송 실패해도 알림은 성공 처리
            }
        } else if (inquiry.getEmail() != null) {
            // 비회원 문의인 경우 이메일로만 발송
            try {
                String replyDate = java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
                        .format(java.time.LocalDateTime.now());
                
                emailService.sendHtmlEmail(
                    inquiry.getEmail(),
                    "문의에 답변이 등록되었습니다",
                    "support-reply-email.html",
                    java.util.Map.of(
                        "TITLE", inquiry.getTitle(),
                        "DATE", replyDate,
                        "CONTENT", content
                    )
                );
            } catch (Exception e) {
                // 이메일 발송 실패는 무시
            }
        }

        return saved.getReplyId();
    }
    
    /**
     * 공개 문의 목록 조회
     */
    public Page<InquiryListDTO> getPublicInquiryList(InquiryCategory category, InquiryStatus status, Pageable pageable) {
        Page<Inquiry> inquiries;
        
        if (status != null) {
            inquiries = inquiryRepository.findByInquiryTypeAndAnswerStatus(InquiryType.PUBLIC, status, pageable);
        } else {
            inquiries = inquiryRepository.findByInquiryType(InquiryType.PUBLIC, pageable);
        }
        
        return inquiries.map(inquiry -> {
            boolean hasReply = inquiryReplyRepository.existsByInquiry(inquiry);
            return inquiryConverter.toListDTO(inquiry, hasReply);
        });
    }
    
    /**
     * 내 문의 내역 조회
     */
    public Page<InquiryListDTO> getMyInquiries(User user, Pageable pageable) {
        Page<Inquiry> inquiries = inquiryRepository.findByUser(user, pageable);
        
        return inquiries.map(inquiry -> {
            boolean hasReply = inquiryReplyRepository.existsByInquiry(inquiry);
            return inquiryConverter.toListDTO(inquiry, hasReply);
        });
    }
    
    /**
     * 문의 상세 조회
     */
    public InquiryDetailDTO getInquiryDetail(Long inquiryId, User user) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._INQUIRY_NOT_FOUND));
        
        // 권한 확인: 공개 문의는 누구나, 비공개 문의는 본인만
        if (inquiry.getInquiryType() == InquiryType.PRIVATE) {
            if (user == null || !inquiry.getUser().getId().equals(user.getId())) {
                throw new GeneralException(ErrorStatus._INQUIRY_ACCESS_DENIED);
            }
        }
        
        // 답변 조회
        InquiryReply reply = inquiryReplyRepository.findByInquiry(inquiry).orElse(null);
        
        return inquiryConverter.toDetailDTO(inquiry, reply);
    }
}

