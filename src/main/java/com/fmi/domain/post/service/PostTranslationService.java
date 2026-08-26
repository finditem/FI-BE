package com.fmi.domain.post.service;

import com.fmi.domain.Enum.LanguageCode;
import com.fmi.domain.user.data.User;
import com.fmi.domain.post.converter.util.PostTranslationConverter;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostTranslation;
import com.fmi.domain.post.repository.PostTranslationRepository;
import com.fmi.domain.post.web.dto.response.PostTranslationResponse;
import com.fmi.external.translation.client.TranslationClient;
import com.fmi.external.translation.util.TranslationTextNormalizer;
import com.fmi.service.UserQueryService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostTranslationService {

    private static final LanguageCode DEFAULT_LANGUAGE = LanguageCode.KO;

    private final PostTranslationRepository postTranslationRepository;
    private final PostQueryService postQueryService;
    private final TranslationClient translationClient;
    private final UserQueryService userQueryService;

    @Transactional
    public PostTranslationResponse getOrTranslate(Long postId, UserDetails userDetails) {
        LanguageCode targetLanguage = resolveTargetLanguage(userDetails);
        Post post = postQueryService.findById(postId);

        return postTranslationRepository
                .findByPost_IdAndLanguageCode(post.getId(), targetLanguage)
                .map(PostTranslationConverter::translate)
                .orElseGet(() -> translateAndCache(post, targetLanguage));
    }

    private LanguageCode resolveTargetLanguage(UserDetails userDetails) {
        if (userDetails == null) {
            return DEFAULT_LANGUAGE;
        }

        User viewer = userQueryService.findUser(userDetails.getUsername());
        return resolveLanguage(viewer.getPreferredLanguage());
    }

    private LanguageCode resolveLanguage(LanguageCode preferredLanguage) {
        return preferredLanguage == null ? DEFAULT_LANGUAGE : preferredLanguage;
    }

    @Transactional
    public void invalidateStaleTranslations(Post post) {
        String currentHash = contentHash(post);
        List<PostTranslation> cached = postTranslationRepository.findAllByPost_Id(post.getId());
        for (PostTranslation translation : cached) {
            if (!Objects.equals(translation.getSourceContentHash(), currentHash)) {
                postTranslationRepository.delete(translation);
            }
        }
    }

    private PostTranslationResponse translateAndCache(Post post, LanguageCode targetLanguage) {
        String translatedTitle = translationClient.translate(post.getTitle(), targetLanguage);
        String translatedContent = translationClient.translate(post.getContent(), targetLanguage);

        PostTranslation saved = postTranslationRepository.save(
                PostTranslation.create(post, targetLanguage, translatedTitle, translatedContent, contentHash(post)));

        return PostTranslationConverter.translate(saved);
    }

    private String contentHash(Post post) {
        String normalizedTitle = TranslationTextNormalizer.normalize(post.getTitle());
        String normalizedContent = TranslationTextNormalizer.normalize(post.getContent());

        String hashSource = "title:"
                + normalizedTitle.length()
                + ":"
                + normalizedTitle
                + "|content:"
                + normalizedContent.length()
                + ":"
                + normalizedContent;

        return TranslationTextNormalizer.hash(hashSource);
    }
}
