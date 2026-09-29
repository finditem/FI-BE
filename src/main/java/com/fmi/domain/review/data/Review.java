package com.fmi.domain.review.data;

import com.fmi.domain.findcompletion.data.FindCompletion;
import com.fmi.domain.user.data.User;
import com.fmi.global.data.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "review",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_completion", columnNames = "find_completion_id"))
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "find_completion_id", nullable = false, updatable = false, unique = true)
    private FindCompletion findCompletion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", updatable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", updatable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "emotion", nullable = false, length = 30)
    private ReviewEmotion emotion;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "review_help_type", joinColumns = @JoinColumn(name = "review_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "help_type", nullable = false, length = 40)
    private Set<ReviewHelpType> helpTypes;

    @Column(name = "content", length = 300)
    private String content;

    @Column(name = "hidden_by_recipient", nullable = false)
    private boolean hiddenByRecipient;

    @Column(name = "revised_at")
    private LocalDateTime revisedAt;

    @Builder
    private Review(
            FindCompletion findCompletion, ReviewEmotion emotion, Set<ReviewHelpType> helpTypes, String content) {
        this.findCompletion = findCompletion;
        this.author = findCompletion.getRequester();
        this.recipient = findCompletion.getHelper();
        this.emotion = emotion;
        this.helpTypes = new HashSet<>(helpTypes);
        this.content = content;
    }

    public Set<ReviewHelpType> getHelpTypes() {
        return Collections.unmodifiableSet(helpTypes);
    }
}
