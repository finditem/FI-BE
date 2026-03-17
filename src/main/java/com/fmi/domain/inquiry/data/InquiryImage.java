package com.fmi.domain.inquiry.data;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inquiry_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class InquiryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "img_url", nullable = false)
    private String imgUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private Inquiry inquiry;

    public static InquiryImage create(String imgUrl, Inquiry inquiry) {
        return new InquiryImage(null, imgUrl, inquiry);
    }
}
