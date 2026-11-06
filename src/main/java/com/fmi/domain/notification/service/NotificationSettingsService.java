package com.fmi.domain.notification.service;

import com.fmi.domain.notification.converter.NotificationConverter;
import com.fmi.domain.notification.data.NotificationSettings;
import com.fmi.domain.notification.repository.NotificationSettingsRepository;
import com.fmi.domain.notification.web.dto.request.NotificationSettingsUpdateRequest;
import com.fmi.domain.notification.web.dto.response.NotificationSettingsResponse;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationSettingsService {

    private final NotificationSettingsRepository notificationSettingsRepository;

    private final NotificationConverter notificationConverter;

    private final UserRepository userRepository;

    /**
     * 알림 설정 조회
     */
    @Transactional
    public NotificationSettingsResponse getSettings(User user) {
        NotificationSettings settings =
                notificationSettingsRepository.findByUser(user).orElseGet(() -> createDefaultSettings(user));

        // User 엔티티를 최신 상태로 조회 (marketingConsent 포함)
        User freshUser = userRepository
                .findById(user.getId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        return notificationConverter.toSettingsDTO(settings, freshUser);
    }

    /**
     * 알림 설정 변경
     */
    @Transactional
    public NotificationSettingsResponse updateSettings(User user, NotificationSettingsUpdateRequest request) {
        // User 엔티티를 최신 상태로 조회
        User freshUser = userRepository
                .findById(user.getId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        NotificationSettings settings =
                notificationSettingsRepository.findByUser(freshUser).orElseGet(() -> createDefaultSettings(freshUser));

        // NotificationSettings 업데이트
        settings.updateSettings(
                request.getCommentEnabled(),
                request.getChatEnabled(),
                request.getInquiryReplyEnabled(),
                request.getReportResultEnabled(),
                request.getFavoriteEnabled(),
                request.getNoticeEnabled(),
                request.getCategoryEnabled(),
                request.getBrowserNotificationEnabled());

        if (request.getEnabledCategories() != null) {
            settings.getEnabledCategories().clear();
            settings.getEnabledCategories().addAll(request.getEnabledCategories());
        }

        NotificationSettings saved = notificationSettingsRepository.save(settings);

        // User 약관/동의 필드 업데이트 (요청에 값이 있는 경우에만)
        boolean userUpdated = false;
        if (request.getTermsOfServiceAgreed() != null) {
            freshUser.setTermsOfServiceAgreed(request.getTermsOfServiceAgreed());
            userUpdated = true;
        }
        if (request.getContentPolicyAgreed() != null) {
            freshUser.setContentPolicyAgreed(request.getContentPolicyAgreed());
            userUpdated = true;
        }
        if (request.getMarketingConsent() != null) {
            freshUser.setMarketingConsent(request.getMarketingConsent());
            userUpdated = true;
        }
        if (userUpdated) {
            userRepository.save(freshUser);
        }

        return notificationConverter.toSettingsDTO(saved, freshUser);
    }

    /**
     * 댓글/채팅/카테고리 설정만 부분 업데이트
     * @deprecated 모든 알림 타입을 변경할 수 있는 updateSettings() 사용 권장
     */
    @Deprecated
    @Transactional
    public NotificationSettingsResponse updateBasicSettings(
            User user, Boolean commentEnabled, Boolean chatEnabled, Boolean categoryEnabled) {
        // User 엔티티를 최신 상태로 조회
        User freshUser = userRepository
                .findById(user.getId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        NotificationSettings settings =
                notificationSettingsRepository.findByUser(freshUser).orElseGet(() -> createDefaultSettings(freshUser));
        settings.updateSettings(commentEnabled, chatEnabled, null, null, null, null, categoryEnabled, null);
        NotificationSettings saved = notificationSettingsRepository.save(settings);
        return notificationConverter.toSettingsDTO(saved, freshUser);
    }

    /**
     * 기본 알림 설정 생성
     */
    @Transactional
    public NotificationSettings createDefaultSettings(User user) {
        NotificationSettings settings =
                NotificationSettings.builder().user(user).build();

        return notificationSettingsRepository.save(settings);
    }
}
