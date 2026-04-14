package com.fmi.domain.notification.web.controller;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.notification.data.PushSubscription;
import com.fmi.domain.notification.repository.PushSubscriptionRepository;
import com.fmi.domain.notification.service.WebPushService;
import com.fmi.domain.notification.web.dto.request.PushSubscribeRequest;
import com.fmi.global.apiPayload.ApiResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
@Tag(name = "Web Push", description = "Web Push 알림 구독 API")
public class PushSubscriptionController {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserRepository userRepository;
    private final WebPushService webPushService;

    @GetMapping("/vapid-key")
    @Operation(summary = "VAPID 공개키 조회", description = "Web Push 구독 시 필요한 VAPID 공개키를 반환합니다")
    public ApiResponse<Map<String, String>> getVapidKey() {
        return ApiResponse.onSuccess(Map.of("publicKey", webPushService.getVapidPublicKey()));
    }

    @PostMapping("/subscribe")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "푸시 구독 등록", description = "브라우저 푸시 구독 정보를 저장합니다")
    @Transactional
    public ApiResponse<String> subscribe(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PushSubscribeRequest request) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        // 이미 같은 endpoint가 등록되어 있으면 무시
        boolean exists = pushSubscriptionRepository.findByUser(user).stream()
                .anyMatch(sub -> sub.getEndpoint().equals(request.getEndpoint()));
        if (exists) {
            return ApiResponse.onSuccess("이미 등록된 구독입니다");
        }

        PushSubscription subscription = PushSubscription.builder()
                .user(user)
                .endpoint(request.getEndpoint())
                .p256dh(request.getP256dh())
                .auth(request.getAuth())
                .build();
        pushSubscriptionRepository.save(subscription);

        return ApiResponse.onSuccess("푸시 구독이 등록되었습니다");
    }

    @DeleteMapping("/subscribe")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "푸시 구독 해제", description = "브라우저 푸시 구독을 해제합니다")
    @Transactional
    public ApiResponse<String> unsubscribe(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String endpoint) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        pushSubscriptionRepository.deleteByUserAndEndpoint(user, endpoint);
        return ApiResponse.onSuccess("푸시 구독이 해제되었습니다");
    }
}
