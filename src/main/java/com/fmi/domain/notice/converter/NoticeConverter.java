package com.fmi.domain.notice.converter;

import com.fmi.domain.notice.data.Notice;
import com.fmi.domain.notice.data.NoticeImage;
import com.fmi.domain.notice.web.dto.NoticeListDTO;
import com.fmi.domain.notice.web.dto.NoticeResponseDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NoticeConverter {

    public NoticeListDTO toListDTO(Notice notice) {
        return toListDTO(notice, null);
    }

    public NoticeListDTO toListDTO(Notice notice, String thumbnailUrl) {
        return NoticeListDTO.builder()
                .noticeId(notice.getNoticeId())
                .title(notice.getTitle())
                .category(notice.getCategory())
                .pinned(notice.getPinned())
                .viewCount(notice.getViewCount())
                .likeCount(notice.getLikeCount())
                .thumbnailUrl(thumbnailUrl)
                .createdAt(notice.getCreatedAt())
                .build();
    }

    public NoticeResponseDTO toResponseDTO(Notice notice) {
        return toResponseDTO(notice, List.of());
    }

    public NoticeResponseDTO toResponseDTO(Notice notice, List<NoticeImage> images) {
        List<String> imageUrls = images.stream()
                .map(NoticeImage::getImgUrl)
                .toList();

        return NoticeResponseDTO.builder()
                .noticeId(notice.getNoticeId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .category(notice.getCategory())
                .pinned(notice.getPinned())
                .viewCount(notice.getViewCount())
                .likeCount(notice.getLikeCount())
                .authorName(notice.getAuthor() != null ? notice.getAuthor().getNickname() : "관리자")
                .images(imageUrls)
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .build();
    }
}

