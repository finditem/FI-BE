package com.fmi.domain.notification.service;

import com.fmi.domain.notification.data.PushSubscription;
import com.fmi.domain.notification.repository.PushSubscriptionRepository;
import com.fmi.domain.notification.web.dto.request.PushSubscribeRequest;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushSubscriptionService {
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserRepository userRepository;

    @Transactional
    public boolean subscribe(UserDetails userDetails, PushSubscribeRequest request) {
        User user = userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        // 이미 같은 endpoint가 등록되어 있으면 무시
        if (pushSubscriptionRepository.existsByUserAndEndpoint(user, request.getEndpoint())) {
            return false;
        }

        PushSubscription subscription = PushSubscription.builder()
                .user(user)
                .endpoint(request.getEndpoint())
                .p256dh(request.getP256dh())
                .auth(request.getAuth())
                .build();
        pushSubscriptionRepository.save(subscription);

        return true;
    }

    @Transactional
    public void unsubscribe(UserDetails userDetails, String endpoint) {
        User user = userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        pushSubscriptionRepository.deleteByUserAndEndpoint(user, endpoint);
    }
}
