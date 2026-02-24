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
import com.fmi.domain.ipblock.service.IpBlacklistService;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
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
    private final StringRedisTemplate stringRedisTemplate;
    private final IpBlacklistService ipBlacklistService;

    /**
     * 1:1 개인 문의 생성
     */
    @Transactional
    public Long createInquiry(InquiryCreateRequestDTO request, UserDetails userDetails, String clientIp) {

        User user = null;
        String resolvedEmail;
        String normalizedIp = normalizeIp(clientIp);

        if (ipBlacklistService.isBlocked(normalizedIp)) {
            throw new GeneralException(ErrorStatus._INQUIRY_IP_BLOCKED);
        }

        if (userDetails != null) {
            user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
            resolvedEmail = user.getEmail();
        } else {
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                throw new GeneralException(ErrorStatus._INQUIRY_GUEST_EMAIL_REQUIRED);
            }
            resolvedEmail = request.getEmail().trim();
            checkGuestRateLimit(normalizedIp);
        }

        Inquiry saved = inquiryRepository.save(
                Inquiry.builder()
                        .title(request.getTitle())
                        .content(request.getContent())
                        .inquiryType(request.getInquiryType() != null ? request.getInquiryType() : InquiryType.ETC)
                        .user(user) // 회원이면 user 세팅
                        .email(user == null ? resolvedEmail : null) // 비회원이면 email 컬럼 세팅
                        .ip(normalizedIp)
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
            log.error("문의 접수 이메일 발송 실패: inquiryId={}", saved.getId(), e);
        }
        
        return saved.getId();
    }

    /**
     * 내 문의 내역 조회
     */
    public Page<InquiryListDTO> getMyInquiries(User user, InquiryStatus status, Boolean answered, Pageable pageable) {
        Page<Inquiry> inquiries;

        if (status != null) {
            inquiries = inquiryRepository.findByUserAndAnswerStatus(user, status, pageable);
        } else if (answered != null) {
            if (answered) {
                inquiries = inquiryRepository.findByUserAndAnswerStatus(user, InquiryStatus.ANSWERED, pageable);
            } else {
                inquiries = inquiryRepository.findByUserAndAnswerStatusNot(user, InquiryStatus.ANSWERED, pageable);
            }
        } else {
            inquiries = inquiryRepository.findByUser(user, pageable);
        }

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
        
        // 권한 확인: 관리자는 모든 문의 조회 가능, 일반 사용자는 본인 문의만
        if (!isAdmin(userDetails)) {
            if (inquiry.getUser() != null) {
                if (!inquiry.getUser().getId().equals(user.getId())) {
                    throw new GeneralException(ErrorStatus._INQUIRY_ACCESS_DENIED);
                }
            } else {
                // 비회원 문의는 관리자만 조회 가능
                throw new GeneralException(ErrorStatus._INQUIRY_ACCESS_DENIED);
            }
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
                log.error("문의 상태 변경 이메일 발송 실패: inquiryId={}", inquiry.getId(), e);
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
                log.error("비회원 문의 상태 변경 이메일 발송 실패: inquiryId={}, email={}", inquiry.getId(), inquiry.getEmail(), e);
            }
        }
    }
    
    /**
     * 상태에 따른 메시지 반환
     */
    private String getStatusMessage(InquiryStatus status) {
        return status.getDescription();
    }

    private String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        return ip.trim();
    }

    private void checkGuestRateLimit(String ip) {
        String countKey = "inquiry:guest:count:" + ip;

        // 요청 횟수 증가
        Long count = stringRedisTemplate.opsForValue().increment(countKey);

        // 첫 요청이면 3분 TTL 설정
        if (count != null && count == 1) {
            stringRedisTemplate.expire(countKey, Duration.ofMinutes(3));
        }

        // 5회 이상이면 자동 블랙리스트 등록
        if (count != null && count >= 5) {
            // 아직 블랙리스트에 없으면 추가
            if (!ipBlacklistService.isBlocked(ip)) {
                ipBlacklistService.blockIp(ip, "자동 차단: 3분 내 5회 이상 문의 요청");
            }
            stringRedisTemplate.delete(countKey);
            throw new GeneralException(ErrorStatus._INQUIRY_IP_BLOCKED);
        }
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

