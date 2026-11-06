package com.fmi.domain.inquiry.inquiry.service;

import com.fmi.domain.inquiry.inquiry.data.Inquiry;
import com.fmi.domain.inquiry.inquiry.data.InquiryImage;
import com.fmi.domain.inquiry.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.inquiry.data.enums.InquiryType;
import com.fmi.domain.inquiry.inquiry.event.InquiryEvent;
import com.fmi.domain.inquiry.inquiry.repository.InquiryImageRepository;
import com.fmi.domain.inquiry.inquiry.repository.InquiryRepository;
import com.fmi.domain.inquiry.inquiry.repository.redis.InquiryGuestRequestRedisRepository;
import com.fmi.domain.inquiry.inquiry.service.internal.InquiryEmailNotifier;
import com.fmi.domain.inquiry.inquiry.web.dto.request.InquiryCreateRequest;
import com.fmi.domain.ipblock.service.IpBlacklistService;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.data.enums.ReferenceType;
import com.fmi.domain.notification.service.NotificationDeliveryService;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.external.storage.S3Service;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final NotificationDeliveryService notificationDeliveryService;

    private final InquiryRepository inquiryRepository;

    private final InquiryImageRepository inquiryImageRepository;

    private final InquiryEmailNotifier inquiryEmailNotifier;

    private final ApplicationEventPublisher eventPublisher;

    private final UserRepository userRepository;

    private final InquiryGuestRequestRedisRepository guestRequestRedisRepository;

    private final IpBlacklistService ipBlacklistService;

    private final S3Service s3Service;

    /**
     * 1:1 개인 문의 생성 (하위 호환)
     */
    @Transactional
    public Long createInquiry(InquiryCreateRequest request, UserDetails userDetails, String clientIp) {
        return createInquiry(request, userDetails, clientIp, null);
    }

    /**
     * 1:1 개인 문의 생성 (이미지 첨부 지원)
     */
    @Transactional
    public Long createInquiry(
            InquiryCreateRequest request, UserDetails userDetails, String clientIp, List<MultipartFile> images) {

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
            inquiryEmailNotifier.sendReceipt(saved);
        } catch (Exception e) {
            log.error("문의 접수 이메일 발송 실패: inquiryId={}", saved.getId(), e);
        }

        return saved.getId();
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
            inquiryEmailNotifier.sendGuestReply(inquiry, content);
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
            notificationDeliveryService.createNotification(
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
        Long count = guestRequestRedisRepository.incrementCount(ip);

        // 5회 이상이면 자동 블랙리스트 등록
        if (count != null && count >= 5) {
            // 아직 블랙리스트에 없으면 추가
            if (!ipBlacklistService.isBlocked(ip)) {
                ipBlacklistService.blockIp(ip, "자동 차단: 3분 내 5회 이상 문의 요청");
            }
            guestRequestRedisRepository.deleteCount(ip);
            throw new GeneralException(ErrorStatus._INQUIRY_IP_BLOCKED);
        }
    }
}
