package com.fmi.domain.inquiry.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.inquiry.converter.InquiryConverter;
import com.fmi.domain.inquiry.data.Inquiry;
import com.fmi.domain.inquiry.data.InquiryImage;
import com.fmi.domain.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.data.enums.InquiryType;
import com.fmi.domain.inquiry.event.InquiryEvent;
import com.fmi.domain.inquiry.repository.InquiryImageRepository;
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
import com.fmi.global.apiPayload.CursorPageResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.service.S3Service;
import com.fmi.service.EmailService;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryImageRepository inquiryImageRepository;
    private final InquiryConverter inquiryConverter;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final InquiryCommentService inquiryCommentService;
    private final StringRedisTemplate stringRedisTemplate;
    private final IpBlacklistService ipBlacklistService;
    private final S3Service s3Service;

    /**
     * 1:1 개인 문의 생성 (하위 호환)
     */
    @Transactional
    public Long createInquiry(InquiryCreateRequestDTO request, UserDetails userDetails, String clientIp) {
        return createInquiry(request, userDetails, clientIp, null);
    }

    /**
     * 1:1 개인 문의 생성 (이미지 첨부 지원)
     */
    @Transactional
    public Long createInquiry(
            InquiryCreateRequestDTO request, UserDetails userDetails, String clientIp, List<MultipartFile> images) {

        User user = null;
        String resolvedEmail;
        String normalizedIp = normalizeIp(clientIp);

        if (ipBlacklistService.isBlocked(normalizedIp)) {
            throw new GeneralException(ErrorStatus._INQUIRY_IP_BLOCKED);
        }

        if (userDetails != null) {
            user = userRepository
                    .findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
            resolvedEmail = user.getEmail();
        } else {
            if (request.getEmail() == null || request.getEmail().isBlank()) {
                throw new GeneralException(ErrorStatus._INQUIRY_GUEST_EMAIL_REQUIRED);
            }
            resolvedEmail = request.getEmail().trim();
            checkGuestRateLimit(normalizedIp);
        }

        Inquiry saved = inquiryRepository.save(Inquiry.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .inquiryType(request.getInquiryType() != null ? request.getInquiryType() : InquiryType.ETC)
                .user(user)
                .email(user == null ? resolvedEmail : null)
                .ip(normalizedIp)
                .build());

        // 이미지 업로드 및 저장
        if (images != null && !images.isEmpty()) {
            List<String> imageUrls = s3Service.upload(images);
            for (String url : imageUrls) {
                inquiryImageRepository.save(InquiryImage.create(url, saved));
            }
        }

        eventPublisher.publishEvent(InquiryEvent.from(saved));

        // 문의 접수 이메일 발송
        try {
            String recipientEmail = saved.getEmail() != null
                    ? saved.getEmail()
                    : (saved.getUser() != null ? saved.getUser().getEmail() : null);
            if (recipientEmail != null) {
                String inquiryDate = java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
                        .format(saved.getCreatedAt() != null ? saved.getCreatedAt() : java.time.LocalDateTime.now());

                String recipientName =
                        saved.getUser() != null && saved.getUser().getNickname() != null
                                ? saved.getUser().getNickname()
                                : recipientEmail;
                emailService.sendHtmlEmailAsync(
                        recipientEmail,
                        "문의가 접수되었습니다",
                        "support-request-email.html",
                        java.util.Map.of(
                                "name",
                                recipientName,
                                "TITLE",
                                saved.getTitle(),
                                "DATE",
                                inquiryDate,
                                "CONTENT",
                                saved.getContent() != null ? saved.getContent() : "",
                                "INQUIRY_ID",
                                String.valueOf(saved.getId())));
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
     * 내 문의 내역 조회 (커서 기반)
     */
    public CursorPageResponse<InquiryListDTO> getMyInquiriesCursor(
            User user, InquiryStatus status, Boolean answered, String keyword, Long cursor, int size) {
        int fetchSize = size + 1;
        Pageable limit = PageRequest.of(0, fetchSize);
        List<Inquiry> inquiries;

        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        if (trimmedKeyword != null) {
            if (status != null) {
                inquiries = inquiryRepository.findByUserAndAnswerStatusAndKeywordCursor(
                        user, status, trimmedKeyword, cursor, limit);
            } else if (answered != null) {
                if (answered) {
                    inquiries = inquiryRepository.findByUserAndAnswerStatusAndKeywordCursor(
                            user, InquiryStatus.ANSWERED, trimmedKeyword, cursor, limit);
                } else {
                    inquiries = inquiryRepository.findByUserAndAnswerStatusNotAndKeywordCursor(
                            user, InquiryStatus.ANSWERED, trimmedKeyword, cursor, limit);
                }
            } else {
                inquiries = inquiryRepository.findByUserAndKeywordCursor(user, trimmedKeyword, cursor, limit);
            }
        } else {
            if (status != null) {
                inquiries = inquiryRepository.findByUserAndAnswerStatusCursor(user, status, cursor, limit);
            } else if (answered != null) {
                if (answered) {
                    inquiries = inquiryRepository.findByUserAndAnswerStatusCursor(
                            user, InquiryStatus.ANSWERED, cursor, limit);
                } else {
                    inquiries = inquiryRepository.findByUserAndAnswerStatusNotCursor(
                            user, InquiryStatus.ANSWERED, cursor, limit);
                }
            } else {
                inquiries = inquiryRepository.findByUserCursor(user, cursor, limit);
            }
        }

        boolean hasNext = inquiries.size() > size;
        List<Inquiry> content = hasNext ? inquiries.subList(0, size) : inquiries;
        Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;

        List<InquiryListDTO> responseList =
                content.stream().map(inquiryConverter::toListDTO).toList();

        return new CursorPageResponse<>(responseList, nextCursor, hasNext);
    }

    /**
     * 문의 상세 조회
     * - 회원 문의: user로 확인
     * - 관리자: 항상 접근 가능
     */
    public InquiryDetailDTO getInquiryDetail(Long inquiryId, UserDetails userDetails) {
        Inquiry inquiry = inquiryRepository
                .findById(inquiryId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._INQUIRY_NOT_FOUND));

        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        User user = userRepository
                .findByEmail(userDetails.getUsername())
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
        List<String> imageUrls = inquiryImageRepository.findByInquiryId(inquiry.getId()).stream()
                .map(InquiryImage::getImgUrl)
                .toList();
        return inquiryConverter.toDetailDTO(inquiry, comments, imageUrls);
    }

    /**
     * 비회원 문의 답변 (이메일 발송)
     */
    @Transactional
    public void replyToGuestInquiry(Long inquiryId, String content) {
        Inquiry inquiry = inquiryRepository
                .findById(inquiryId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._INQUIRY_NOT_FOUND));

        if (inquiry.getUser() != null) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }

        if (inquiry.getEmail() == null || inquiry.getEmail().isBlank()) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }

        inquiry.markAsAnswered();

        try {
            String replyDate = java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
                    .format(java.time.LocalDateTime.now());

            emailService.sendHtmlEmailAsync(
                    inquiry.getEmail(),
                    "문의에 대한 답변이 도착했습니다",
                    "support-reply-email.html",
                    java.util.Map.of(
                            "name", inquiry.getEmail(),
                            "TITLE", inquiry.getTitle(),
                            "DATE", replyDate,
                            "CONTENT", content,
                            "INQUIRY_ID", String.valueOf(inquiry.getId())));
        } catch (Exception e) {
            log.error("비회원 문의 답변 이메일 발송 실패: inquiryId={}, email={}", inquiry.getId(), inquiry.getEmail(), e);
        }
    }

    /**
     * 문의 상태 변경(관리자)
     */
    @Transactional
    public void updateStatus(Long inquiryId, InquiryStatus status) {
        Inquiry inquiry = inquiryRepository
                .findById(inquiryId)
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
        if (inquiry.getUser() != null) {
            String statusMessage = getStatusMessage(status);
            notificationService.createNotification(
                    inquiry.getUser(),
                    NotificationType.INQUIRY_REPLY,
                    "문의 상태가 변경되었습니다",
                    statusMessage,
                    ReferenceType.INQUIRY,
                    inquiry.getId());
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
