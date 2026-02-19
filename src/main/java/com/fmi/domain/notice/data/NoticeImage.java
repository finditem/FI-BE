package com.fmi.domain.notice.data;

import com.fmi.domain.post.data.ImageType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notice_image")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NoticeImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "img_url", nullable = false)
    private String imgUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false)
    private ImageType imageType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    private NoticeImage(String imgUrl, ImageType imageType, Notice notice) {
        this.imgUrl = imgUrl;
        this.imageType = imageType;
        this.notice = notice;
    }

    public static NoticeImage create(String imgUrl, ImageType imageType, Notice notice) {
        return new NoticeImage(imgUrl, imageType, notice);
    }
}
