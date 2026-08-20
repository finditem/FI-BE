package com.fmi.domain.user.web.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TermsAgreeRequest {

    @AssertTrue(message = "개인정보 처리방침에 동의해주세요") private boolean privacyPolicyAgreed;

    @AssertTrue(message = "이용약관에 동의해주세요") private boolean termsOfServiceAgreed;

    private boolean contentPolicyAgreed;

    private boolean marketingConsent;
}
