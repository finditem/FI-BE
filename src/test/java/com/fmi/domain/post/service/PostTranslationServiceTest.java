package com.fmi.domain.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fmi.domain.Enum.LanguageCode;
import com.fmi.domain.user.data.User;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostTranslation;
import com.fmi.domain.post.repository.PostTranslationRepository;
import com.fmi.domain.post.web.dto.response.PostTranslationResponse;
import com.fmi.external.translation.client.TranslationClient;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.service.UserQueryService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class PostTranslationServiceTest {

    @Mock
    private PostTranslationRepository postTranslationRepository;

    @Mock
    private PostQueryService postQueryService;

    @Mock
    private TranslationClient translationClient;

    @Mock
    private UserQueryService userQueryService;

    @InjectMocks
    private PostTranslationService postTranslationService;

    private static final Long POST_ID = 1L;
    private static final String VIEWER_EMAIL = "viewer@test.com";

    private Post post;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        post = new Post();
        post.setId(POST_ID);
        post.setTitle("지갑을 잃어버렸어요");
        post.setContent("강남역 2번 출구 근처에서 검정색 지갑을 잃어버렸습니다.");

        userDetails = new org.springframework.security.core.userdetails.User(VIEWER_EMAIL, "password", List.of());
    }

    private User viewerWithLanguage(LanguageCode languageCode) {
        return User.builder()
                .id(2L)
                .email(VIEWER_EMAIL)
                .nickname("조회자")
                .preferredLanguage(languageCode)
                .build();
    }

    private PostTranslation translationOf(LanguageCode languageCode, String title, String content, String hash) {
        return PostTranslation.create(post, languageCode, title, content, hash);
    }

    /** 서비스가 실제로 계산하는 해시를 얻는다. (해시 계산식을 테스트에 중복 구현하지 않기 위함) */
    private String currentHashOf(Post target) {
        PostTranslationRepository repository = org.mockito.Mockito.mock(PostTranslationRepository.class);
        PostQueryService queryService = org.mockito.Mockito.mock(PostQueryService.class);
        TranslationClient client = org.mockito.Mockito.mock(TranslationClient.class);
        PostTranslationService service = new PostTranslationService(repository, queryService, client, userQueryService);

        given(queryService.findById(target.getId())).willReturn(target);
        given(repository.findByPost_IdAndLanguageCode(target.getId(), LanguageCode.KO))
                .willReturn(Optional.empty());
        given(client.translate(anyString(), eq(LanguageCode.KO))).willReturn("translated");
        given(repository.save(any(PostTranslation.class))).willAnswer(invocation -> invocation.getArgument(0));

        service.getOrTranslate(target.getId(), null);

        ArgumentCaptor<PostTranslation> captor = ArgumentCaptor.forClass(PostTranslation.class);
        verify(repository).save(captor.capture());
        return captor.getValue().getSourceContentHash();
    }

    @Nested
    @DisplayName("getOrTranslate")
    class GetOrTranslate {

        @Test
        @DisplayName("저장된 번역이 있으면 외부 번역 API를 호출하지 않고 저장된 값을 반환한다")
        void returnsCachedTranslationWithoutCallingApi() {
            given(postQueryService.findById(POST_ID)).willReturn(post);
            given(userQueryService.findUser(VIEWER_EMAIL)).willReturn(viewerWithLanguage(LanguageCode.EN));
            given(postTranslationRepository.findByPost_IdAndLanguageCode(POST_ID, LanguageCode.EN))
                    .willReturn(
                            Optional.of(translationOf(LanguageCode.EN, "Lost my wallet", "Lost near exit 2", "hash")));

            PostTranslationResponse response = postTranslationService.getOrTranslate(POST_ID, userDetails);

            assertThat(response.getPostId()).isEqualTo(POST_ID);
            assertThat(response.getLanguageCode()).isEqualTo(LanguageCode.EN);
            assertThat(response.getTranslatedTitle()).isEqualTo("Lost my wallet");
            assertThat(response.getTranslatedContent()).isEqualTo("Lost near exit 2");
            verifyNoInteractions(translationClient);
            verify(postTranslationRepository, never()).save(any());
        }

        @Test
        @DisplayName("저장된 번역이 없으면 제목/본문을 번역해 저장하고 반환한다")
        void translatesAndCachesWhenNotCached() {
            given(postQueryService.findById(POST_ID)).willReturn(post);
            given(userQueryService.findUser(VIEWER_EMAIL)).willReturn(viewerWithLanguage(LanguageCode.EN));
            given(postTranslationRepository.findByPost_IdAndLanguageCode(POST_ID, LanguageCode.EN))
                    .willReturn(Optional.empty());
            given(translationClient.translate(post.getTitle(), LanguageCode.EN)).willReturn("I lost my wallet");
            given(translationClient.translate(post.getContent(), LanguageCode.EN))
                    .willReturn("I lost a black wallet near exit 2 of Gangnam station.");
            given(postTranslationRepository.save(any(PostTranslation.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            PostTranslationResponse response = postTranslationService.getOrTranslate(POST_ID, userDetails);

            ArgumentCaptor<PostTranslation> captor = ArgumentCaptor.forClass(PostTranslation.class);
            verify(postTranslationRepository).save(captor.capture());
            PostTranslation saved = captor.getValue();

            assertThat(saved.getPost()).isSameAs(post);
            assertThat(saved.getLanguageCode()).isEqualTo(LanguageCode.EN);
            assertThat(saved.getTranslatedTitle()).isEqualTo("I lost my wallet");
            assertThat(saved.getSourceContentHash()).isNotBlank();
            assertThat(response.getTranslatedTitle()).isEqualTo("I lost my wallet");
            assertThat(response.getTranslatedContent())
                    .isEqualTo("I lost a black wallet near exit 2 of Gangnam station.");
        }

        @Test
        @DisplayName("비로그인 조회자는 기본 언어(KO)로 번역하며 회원 조회를 하지 않는다")
        void usesDefaultLanguageForAnonymousViewer() {
            given(postQueryService.findById(POST_ID)).willReturn(post);
            given(postTranslationRepository.findByPost_IdAndLanguageCode(POST_ID, LanguageCode.KO))
                    .willReturn(Optional.of(translationOf(LanguageCode.KO, "제목", "본문", "hash")));

            PostTranslationResponse response = postTranslationService.getOrTranslate(POST_ID, null);

            assertThat(response.getLanguageCode()).isEqualTo(LanguageCode.KO);
            verifyNoInteractions(userQueryService);
        }

        @Test
        @DisplayName("선호 언어가 설정되지 않은 회원은 기본 언어(KO)로 번역한다")
        void usesDefaultLanguageWhenPreferredLanguageIsNull() {
            given(postQueryService.findById(POST_ID)).willReturn(post);
            given(userQueryService.findUser(VIEWER_EMAIL)).willReturn(viewerWithLanguage(null));
            given(postTranslationRepository.findByPost_IdAndLanguageCode(POST_ID, LanguageCode.KO))
                    .willReturn(Optional.of(translationOf(LanguageCode.KO, "제목", "본문", "hash")));

            PostTranslationResponse response = postTranslationService.getOrTranslate(POST_ID, userDetails);

            assertThat(response.getLanguageCode()).isEqualTo(LanguageCode.KO);
            verify(postTranslationRepository).findByPost_IdAndLanguageCode(POST_ID, LanguageCode.KO);
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 PostQueryService의 GeneralException(POST404-NOT_FOUND)이 그대로 전파된다")
        void throwsWhenPostNotFound() {
            given(postQueryService.findById(POST_ID)).willThrow(new GeneralException(ErrorStatus._POST_NOT_FOUND));

            assertThatThrownBy(() -> postTranslationService.getOrTranslate(POST_ID, null))
                    .isInstanceOf(GeneralException.class)
                    .extracting(e -> ((GeneralException) e).getCode())
                    .isEqualTo(ErrorStatus._POST_NOT_FOUND);

            verifyNoInteractions(translationClient);
        }
    }

    @Nested
    @DisplayName("invalidateStaleTranslations")
    class InvalidateStaleTranslations {

        @Test
        @DisplayName("본문이 실질적으로 바뀌어 해시가 다르면 저장된 번역을 삭제한다")
        void deletesTranslationWithDifferentHash() {
            PostTranslation stale = translationOf(LanguageCode.EN, "old title", "old content", "outdated-hash");
            given(postTranslationRepository.findAllByPost_Id(POST_ID)).willReturn(List.of(stale));

            postTranslationService.invalidateStaleTranslations(post);

            verify(postTranslationRepository).delete(stale);
        }

        @Test
        @DisplayName("해시가 같으면 저장된 번역을 유지한다")
        void keepsTranslationWithSameHash() {
            String currentHash = currentHashOf(post);
            PostTranslation fresh = translationOf(LanguageCode.EN, "title", "content", currentHash);
            given(postTranslationRepository.findAllByPost_Id(POST_ID)).willReturn(List.of(fresh));

            postTranslationService.invalidateStaleTranslations(post);

            verify(postTranslationRepository, never()).delete(any());
        }

        @Test
        @DisplayName("특수문자·공백만 달라진 경우는 실질적 변경이 아니므로 저장된 번역을 유지한다")
        void keepsTranslationWhenOnlyWhitespaceOrSymbolsChanged() {
            String hashBeforeEdit = currentHashOf(post);

            post.setTitle("  지갑을   잃어버렸어요!!! ");
            post.setContent("강남역 2번 출구 근처에서,\n\n검정색 지갑을 잃어버렸습니다...");

            PostTranslation cached = translationOf(LanguageCode.EN, "title", "content", hashBeforeEdit);
            given(postTranslationRepository.findAllByPost_Id(POST_ID)).willReturn(List.of(cached));

            postTranslationService.invalidateStaleTranslations(post);

            verify(postTranslationRepository, never()).delete(any());
        }

        @Test
        @DisplayName("여러 언어의 번역 중 해시가 다른 것만 삭제한다")
        void deletesOnlyStaleOnes() {
            String currentHash = currentHashOf(post);
            PostTranslation fresh = translationOf(LanguageCode.EN, "fresh", "fresh", currentHash);
            PostTranslation stale = translationOf(LanguageCode.KO, "stale", "stale", "outdated-hash");
            given(postTranslationRepository.findAllByPost_Id(POST_ID)).willReturn(List.of(fresh, stale));

            postTranslationService.invalidateStaleTranslations(post);

            verify(postTranslationRepository).delete(stale);
            verify(postTranslationRepository, never()).delete(fresh);
        }

        @Test
        @DisplayName("저장된 번역이 없으면 아무것도 삭제하지 않는다")
        void doesNothingWhenNoCachedTranslation() {
            given(postTranslationRepository.findAllByPost_Id(POST_ID)).willReturn(List.of());

            postTranslationService.invalidateStaleTranslations(post);

            verify(postTranslationRepository, never()).delete(any());
        }
    }
}
