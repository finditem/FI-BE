package com.fmi.domain.auth.service.result;

import com.fmi.domain.auth.data.IssuedTokens;

public record TokenIssueResult(IssuedTokens issuedTokens, String refreshTokenId) {}
