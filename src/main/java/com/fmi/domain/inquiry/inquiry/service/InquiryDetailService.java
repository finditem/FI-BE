package com.fmi.domain.inquiry.inquiry.service;

import com.fmi.domain.inquiry.inquiry.web.dto.response.InquiryDetailResponse;
import com.fmi.domain.notification.data.enums.NotificationType;
import com.fmi.domain.notification.service.NotificationService;
import com.fmi.domain.user.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InquiryDetailService {
    private final InquiryQueryService inquiryQueryService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public InquiryDetailResponse getInquiryDetail(Long inquiryId, UserDetails userDetails) {
        InquiryDetailResponse inquiry = inquiryQueryService.getInquiryDetail(inquiryId, userDetails);

        // 문의 조회 시 관련 알림 자동 읽음처리
        if (userDetails != null) {
            userRepository
                    .findByEmail(userDetails.getUsername())
                    .ifPresent(user -> notificationService.markNotificationsAsReadByReference(
                            user, inquiryId, List.of(NotificationType.COMMENT, NotificationType.INQUIRY_REPLY)));
        }

        return inquiry;
    }
}
