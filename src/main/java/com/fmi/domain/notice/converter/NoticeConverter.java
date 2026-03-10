package com.fmi.domain.notice.converter;

import com.fmi.domain.notice.data.Notice;
import com.fmi.domain.notice.data.NoticeImage;
import com.fmi.domain.notice.web.dto.NoticeListDTO;
import com.fmi.domain.notice.web.dto.NoticeResponseDTO;
import com.fmi.domain.post.data.ImageType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class NoticeConverter {

    public NoticeListDTO toListDTO(Notice notice, String thumbnailUrl, boolean isHot, int commentCount) {
        return NoticeListDTO.builder()
                .noticeId(notice.getNoticeId())
                .title(notice.getTitle())
                .category(notice.getCategory())
                .pinned(notice.getPinned())
                .viewCount(notice.getViewCount())
                .likeCount(notice.getLikeCount())
                .commentCount(commentCount)
                .thumbnailUrl(thumbnailUrl)
                .isNew(notice.isNew())
                .isHot(isHot)
                .createdAt(notice.getCreatedAt())
                .build();
    }

    public NoticeListDTO toListDTO(Notice notice, String thumbnailUrl) {
        return toListDTO(notice, thumbnailUrl, false, 0);
    }

    public NoticeListDTO toListDTO(Notice notice) {
        return toListDTO(notice, null, false, 0);
    }

    public NoticeResponseDTO toResponseDTO(Notice notice) {
        return toResponseDTO(notice, List.of());
    }

    public NoticeResponseDTO toResponseDTO(Notice notice, List<NoticeImage> images) {
        return toResponseDTO(notice, images, 0, false);
    }

    public NoticeResponseDTO toResponseDTO(Notice notice, List<NoticeImage> images, int commentCount) {
        return toResponseDTO(notice, images, commentCount, false);
    }

    public NoticeResponseDTO toResponseDTO(Notice notice, List<NoticeImage> images, int commentCount, boolean isHot) {
        List<String> imageUrls = images.stream()
                .map(NoticeImage::getImgUrl)
                .toList();

        String thumbnailUrl = images.stream()
                .filter(img -> img.getImageType() == ImageType.THUMBNAIL)
                .map(NoticeImage::getImgUrl)
                .findFirst()
                .orElse(imageUrls.isEmpty() ? null : imageUrls.get(0));

        String summary = notice.getContent() != null && notice.getContent().length() > 100
                ? notice.getContent().substring(0, 100) + "..."
                : notice.getContent();

        return NoticeResponseDTO.builder()
                .noticeId(notice.getNoticeId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .summary(summary)
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
