package com.fmi.domain.auth.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmailVerifyResponse {
    private boolean verified;
}

