package com.fmi.domain.notice.notice.converter;

import com.fmi.domain.notice.notice.data.Notice;
import com.fmi.domain.notice.notice.data.NoticeImage;
import com.fmi.domain.notice.notice.web.dto.response.NoticeListResponse;
import com.fmi.domain.notice.notice.web.dto.response.NoticeResponse;
import com.fmi.domain.post.post.data.enums.ImageType;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NoticeConverter {

    public NoticeListResponse toListDTO(Notice notice, String thumbnailUrl, boolean isHot, int commentCount) {
        String summary = notice.getContent() != null && notice.getContent().length() > 100
                ? notice.getContent().substring(0, 100) + "..."
                : notice.getContent();

        return NoticeListResponse.builder()
                .noticeId(notice.getNoticeId())
                .title(notice.getTitle())
                .category(notice.getCategory())
                .pinned(notice.getPinned())
                .viewCount(notice.getViewCount())
                .likeCount(notice.getLikeCount())
                .commentCount(commentCount)
                .thumbnailUrl(thumbnailUrl)
                .summary(summary)
                .isNew(notice.isNew())
                .isHot(isHot)
                .createdAt(notice.getCreatedAt())
                .build();
    }

    public NoticeListResponse toListDTO(Notice notice, String thumbnailUrl) {
        return toListDTO(notice, thumbnailUrl, false, 0);
    }

    public NoticeListResponse toListDTO(Notice notice) {
        return toListDTO(notice, null, false, 0);
    }

    public NoticeResponse toResponseDTO(Notice notice) {
        return toResponseDTO(notice, List.of());
    }

    public NoticeResponse toResponseDTO(Notice notice, List<NoticeImage> images) {
        return toResponseDTO(notice, images, 0, false);
    }

    public NoticeResponse toResponseDTO(Notice notice, List<NoticeImage> images, int commentCount) {
        return toResponseDTO(notice, images, commentCount, false);
    }

    public NoticeResponse toResponseDTO(Notice notice, List<NoticeImage> images, int commentCount, boolean isHot) {
        List<String> imageUrls = images.stream().map(NoticeImage::getImgUrl).toList();

        String thumbnailUrl = images.stream()
                .filter(img -> img.getImageType() == ImageType.THUMBNAIL)
                .map(NoticeImage::getImgUrl)
                .findFirst()
                .orElse(imageUrls.isEmpty() ? null : imageUrls.get(0));

        return NoticeResponse.builder()
                .noticeId(notice.getNoticeId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .category(notice.getCategory())
                .pinned(notice.getPinned())
                .viewCount(notice.getViewCount())
                .likeCount(notice.getLikeCount())
                .commentCount(commentCount)
                .authorName(notice.getAuthor() != null ? notice.getAuthor().getNickname() : "관리자")
                .thumbnailUrl(thumbnailUrl)
                .images(imageUrls)
                .isNew(notice.isNew())
                .isHot(isHot)
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .build();
    }
}
