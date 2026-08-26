package com.fmi.domain.inquiry.data;

import com.fmi.domain.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.data.enums.InquiryType;
import com.fmi.domain.user.data.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "customer_inquiry")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "inquiry_type", nullable = false, length = 30)
    private InquiryType inquiryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_status", nullable = false, length = 30)
    @Builder.Default
    private InquiryStatus answerStatus = InquiryStatus.RECEIVED;

    @Column(length = 255)
    private String email; // 비회원 문의 시 이메일

    @Column(nullable = false)
    @Builder.Default
    private Boolean answered = false;

    @Column(length = 45)
    private String ip; // 비회원 문의 시 IP

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void markAsAnswered() {
        this.answerStatus = InquiryStatus.ANSWERED;
    }

    public void markAsPending() {
        this.answerStatus = InquiryStatus.PENDING;
    }

    public void markAsReceived() {
        this.answerStatus = InquiryStatus.RECEIVED;
    }

    public void updateAnswered(boolean answered) {
        this.answered = answered;
    }
}
