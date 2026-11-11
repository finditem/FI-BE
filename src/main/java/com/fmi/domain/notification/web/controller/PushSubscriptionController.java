package com.fmi.domain.notification.web.controller;

import com.fmi.domain.notification.service.PushSubscriptionService;
import com.fmi.domain.notification.service.WebPushService;
import com.fmi.domain.notification.web.dto.request.PushSubscribeRequest;
import com.fmi.domain.notification.web.swagger.PushSubscriptionSwagger;
import com.fmi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/push")
@RequiredArgsConstructor
public class PushSubscriptionController implements PushSubscriptionSwagger {

    private final PushSubscriptionService pushSubscriptionService;
    private final WebPushService webPushService;

    /**
     * VAPID 공개키 조회
     * GET /push/vapid-key
     */
    @GetMapping("/vapid-key")
    @Override
    public ApiResponse<Map<String, String>> getVapidKey() {
        return ApiResponse.onSuccess(Map.of("publicKey", webPushService.getVapidPublicKey()));
    }

    /**
     * 푸시 구독 등록
     * POST /push/subscribe
     */
    @PostMapping("/subscribe")
    @PreAuthorize("isAuthenticated()")
    @Override
    public ApiResponse<String> subscribe(
            @AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody PushSubscribeRequest request) {

        boolean subscribed = pushSubscriptionService.subscribe(userDetails, request);
        if (!subscribed) {
            return ApiResponse.onSuccess("이미 등록된 구독입니다");
        }
        return ApiResponse.onSuccess("푸시 구독이 등록되었습니다");
    }

    /**
     * 푸시 구독 해제
     * DELETE /push/subscribe
     */
    @DeleteMapping("/subscribe")
    @PreAuthorize("isAuthenticated()")
    @Override
    public ApiResponse<String> unsubscribe(
            @AuthenticationPrincipal UserDetails userDetails, @RequestParam String endpoint) {

        pushSubscriptionService.unsubscribe(userDetails, endpoint);
        return ApiResponse.onSuccess("푸시 구독이 해제되었습니다");
    }
}
