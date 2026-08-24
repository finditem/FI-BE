package com.fmi.domain.post.data;

import com.fmi.domain.Enum.LanguageCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "post_translation", uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "language_code"}))
public class PostTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(name = "language_code", nullable = false)
    private LanguageCode languageCode;

    @Column(name = "translated_title", nullable = false, length = 100)
    private String translatedTitle;

    @Column(name = "translated_content", nullable = false, length = 1000)
    private String translatedContent;

    @Column(name = "source_content_hash", nullable = false, length = 64)
    private String sourceContentHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static PostTranslation create(
            Post post,
            LanguageCode languageCode,
            String translatedTitle,
            String translatedContent,
            String sourceContentHash) {
        return PostTranslation.builder()
                .post(post)
                .languageCode(languageCode)
                .translatedTitle(translatedTitle)
                .translatedContent(translatedContent)
                .sourceContentHash(sourceContentHash)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
