package com.fmi.domain.auth.service.result;

import com.fmi.domain.user.user.data.User;

public record AuthenticateResult(User user, boolean isTemporaryPassword) {}
