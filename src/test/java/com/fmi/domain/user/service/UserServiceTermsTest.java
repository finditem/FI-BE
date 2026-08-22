package com.fmi.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.domain.user.web.dto.TermsAgreeRequest;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTermsTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("약관 동의")
    class AgreeTerms {

        @Nested
        @DisplayName("사용자가 약관 동의를 요청하면")
        class WithTermsAgreementRequest {

            @Test
            @DisplayName("네 가지 동의 값을 사용자에게 저장한다")
            void storesAllFourAgreementValues() {
                String email = "member@finditem.kr";
                User user = User.builder().email(email).build();
                TermsAgreeRequest request = new TermsAgreeRequest();
                request.setPrivacyPolicyAgreed(true);
                request.setTermsOfServiceAgreed(true);
                request.setContentPolicyAgreed(true);
                request.setMarketingConsent(false);
                when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

                userService.agreeTerms(email, request);

                verify(userRepository).save(user);
                assertThat(user.isPrivacyPolicyAgreed()).isTrue();
                assertThat(user.isTermsOfServiceAgreed()).isTrue();
                assertThat(user.isContentPolicyAgreed()).isTrue();
                assertThat(user.isMarketingConsent()).isFalse();
            }
        }
    }
}
