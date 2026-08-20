package com.fmi.domain.notification.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PushSubscribeRequest {

    @NotBlank(message = "endpoint는 필수입니다") private String endpoint;

    @NotBlank(message = "p256dh는 필수입니다") private String p256dh;

    @NotBlank(message = "auth는 필수입니다") private String auth;
}
