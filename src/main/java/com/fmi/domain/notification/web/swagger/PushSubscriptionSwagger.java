package com.fmi.domain.notification.web.swagger;

import com.fmi.domain.notification.web.dto.request.PushSubscribeRequest;
import com.fmi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.security.core.userdetails.UserDetails;

@Tag(name = "Web Push", description = "Web Push 알림 구독 API")
public interface PushSubscriptionSwagger {

    @Operation(
            summary = "VAPID 공개키 조회",
            description = "Web Push 구독 시 필요한 VAPID 공개키를 반환합니다. "
                    + "프론트엔드에서 pushManager.subscribe() 호출 시 applicationServerKey로 사용합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "VAPID 공개키 조회 성공")
    })
    ApiResponse<Map<String, String>> getVapidKey();

    @Operation(
            summary = "푸시 구독 등록",
            description = "브라우저 Service Worker에서 생성한 푸시 구독 정보를 저장합니다. " + "동일한 endpoint가 이미 등록되어 있으면 중복 저장하지 않습니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "푸시 구독 등록 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "USER404-NOT_FOUND: 사용자를 찾을 수 없습니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"USER404-NOT_FOUND\", \"message\": \"사용자를 찾을 수 없습니다.\"}")))
    })
    ApiResponse<String> subscribe(@Parameter(hidden = true) UserDetails userDetails, PushSubscribeRequest request);

    @Operation(summary = "푸시 구독 해제", description = "브라우저 푸시 구독을 해제합니다. 로그아웃 시 호출하여 불필요한 푸시 발송을 방지합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "푸시 구독 해제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "USER404-NOT_FOUND: 사용자를 찾을 수 없습니다",
                content =
                        @Content(
                                mediaType = "application/json",
                                examples =
                                        @ExampleObject(
                                                value =
                                                        "{\"isSuccess\": false, \"code\": \"USER404-NOT_FOUND\", \"message\": \"사용자를 찾을 수 없습니다.\"}")))
    })
    ApiResponse<String> unsubscribe(
            @Parameter(hidden = true) UserDetails userDetails,
            @Parameter(description = "구독 해제할 푸시 endpoint URL") String endpoint);
}
