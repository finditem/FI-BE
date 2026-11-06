package com.fmi.domain.notice.notice.service;

import com.fmi.domain.notice.comment.repository.NoticeCommentRepository;
import com.fmi.domain.notice.notice.converter.NoticeConverter;
import com.fmi.domain.notice.notice.data.Notice;
import com.fmi.domain.notice.notice.data.NoticeImage;
import com.fmi.domain.notice.notice.data.enums.NoticeCategory;
import com.fmi.domain.notice.notice.data.enums.NoticeSortType;
import com.fmi.domain.notice.notice.repository.NoticeImageRepository;
import com.fmi.domain.notice.notice.repository.NoticeLikeRepository;
import com.fmi.domain.notice.notice.repository.NoticeRepository;
import com.fmi.domain.notice.notice.repository.redis.NoticeViewRedisRepository;
import com.fmi.domain.notice.notice.web.dto.response.NoticeListResponse;
import com.fmi.domain.notice.notice.web.dto.response.NoticeMetaResponse;
import com.fmi.domain.notice.notice.web.dto.response.NoticeResponse;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.global.apiPayload.CursorPageResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeQueryService {

    private final NoticeRepository noticeRepository;

    private final NoticeImageRepository noticeImageRepository;

    private final NoticeLikeRepository noticeLikeRepository;

    private final UserRepository userRepository;

    private final NoticeConverter noticeConverter;

    private final NoticeCommentRepository noticeCommentRepository;

    private final NoticeViewRedisRepository noticeViewRedisRepository;

    /**
     * 공지사항 목록 조회 (카테고리/키워드 필터)
     */
    public Page<NoticeListResponse> getNoticeList(
            NoticeCategory category, String keyword, NoticeSortType sortType, Pageable pageable) {
        Page<Notice> notices;
        if (keyword != null && !keyword.isBlank()) {
            String sanitized = sanitizeFulltextKeyword(keyword);
            if (sanitized.isEmpty()) {
                // 특수문자만 입력된 경우 키워드 없는 조회로 폴백
                if (category != null) {
                    notices = noticeRepository.findByDraftFalseAndCategory(category, pageable);
                } else {
                    notices = noticeRepository.findByDraftFalse(pageable);
                }
            } else {
                String categoryStr = category != null ? category.name() : null;
                Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
                notices = switch (sortType) {
                    case OLDEST -> noticeRepository.searchNoticesOldest(categoryStr, sanitized, unsorted);
                    case MOST_VIEWED -> noticeRepository.searchNoticesMostViewed(categoryStr, sanitized, unsorted);
                    default -> noticeRepository.searchNotices(categoryStr, sanitized, unsorted);
                };
            }
        } else if (category != null) {
            notices = noticeRepository.findByDraftFalseAndCategory(category, pageable);
        } else {
            notices = noticeRepository.findByDraftFalse(pageable);
        }

        // 썸네일 일괄 조회 (N+1 방지)
        List<Long> noticeIds =
                notices.getContent().stream().map(Notice::getNoticeId).toList();

        Map<Long, String> thumbnailMap = Map.of();
        if (!noticeIds.isEmpty()) {
            thumbnailMap = noticeImageRepository.findThumbnailsByNoticeIds(noticeIds).stream()
                    .collect(Collectors.toMap(
                            img -> img.getNotice().getNoticeId(), NoticeImage::getImgUrl, (a, b) -> a));
        }

        // HOT 공지 ID 조회
        Set<Long> hotNoticeIds = getHotNoticeIds();

        // 댓글 수 일괄 조회
        Map<Long, Long> commentCountMap = Map.of();
        if (!noticeIds.isEmpty()) {
            commentCountMap = noticeCommentRepository.countByNoticeIds(noticeIds).stream()
                    .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        }

        Map<Long, String> finalThumbnailMap = thumbnailMap;
        Map<Long, Long> finalCommentCountMap = commentCountMap;
        return notices.map(notice -> noticeConverter.toListDTO(
                notice,
                finalThumbnailMap.get(notice.getNoticeId()),
                hotNoticeIds.contains(notice.getNoticeId()),
                finalCommentCountMap.getOrDefault(notice.getNoticeId(), 0L).intValue()));
    }

    /**
     * 공지사항 목록 조회 (커서 기반)
     */
    public CursorPageResponse<NoticeListResponse> getNoticeListCursor(
            NoticeCategory category, String keyword, NoticeSortType sortType, Long cursor, int size) {
        int fetchSize = size + 1;
        String categoryStr = category != null ? category.name() : null;

        List<Notice> notices;
        if (keyword != null && !keyword.isBlank()) {
            String sanitized = sanitizeFulltextKeyword(keyword);
            if (sanitized.isEmpty()) {
                // 특수문자만 입력된 경우 키워드 없는 조회로 폴백
                notices = switch (sortType) {
                    case OLDEST -> noticeRepository.findByDraftFalseOldestCursor(categoryStr, cursor, fetchSize);
                    default -> noticeRepository.findByDraftFalseCursor(categoryStr, cursor, fetchSize);
                };
            } else {
                notices = switch (sortType) {
                    case OLDEST ->
                        noticeRepository.searchNoticesOldestCursor(categoryStr, sanitized, cursor, fetchSize);
                    case MOST_VIEWED ->
                        noticeRepository.searchNoticesMostViewedCursor(categoryStr, sanitized, cursor, fetchSize);
                    default -> noticeRepository.searchNoticesCursor(categoryStr, sanitized, cursor, fetchSize);
                };
            }
        } else {
            notices = switch (sortType) {
                case OLDEST -> noticeRepository.findByDraftFalseOldestCursor(categoryStr, cursor, fetchSize);
                default -> noticeRepository.findByDraftFalseCursor(categoryStr, cursor, fetchSize);
            };
        }

        boolean hasNext = notices.size() > size;
        List<Notice> content = hasNext ? notices.subList(0, size) : notices;
        Long nextCursor = hasNext ? content.get(content.size() - 1).getNoticeId() : null;

        // 썸네일 일괄 조회 (N+1 방지)
        List<Long> noticeIds = content.stream().map(Notice::getNoticeId).toList();

        Map<Long, String> thumbnailMap = Map.of();
        if (!noticeIds.isEmpty()) {
            thumbnailMap = noticeImageRepository.findThumbnailsByNoticeIds(noticeIds).stream()
                    .collect(Collectors.toMap(
                            img -> img.getNotice().getNoticeId(), NoticeImage::getImgUrl, (a, b) -> a));
        }

        // HOT 공지 ID 조회
        Set<Long> hotNoticeIds = getHotNoticeIds();

        // 댓글 수 일괄 조회
        Map<Long, Long> commentCountMap = Map.of();
        if (!noticeIds.isEmpty()) {
            commentCountMap = noticeCommentRepository.countByNoticeIds(noticeIds).stream()
                    .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        }

        Map<Long, String> finalThumbnailMap = thumbnailMap;
        Map<Long, Long> finalCommentCountMap = commentCountMap;
        List<NoticeListResponse> responseList = content.stream()
                .map(notice -> noticeConverter.toListDTO(
                        notice,
                        finalThumbnailMap.get(notice.getNoticeId()),
                        hotNoticeIds.contains(notice.getNoticeId()),
                        finalCommentCountMap
                                .getOrDefault(notice.getNoticeId(), 0L)
                                .intValue()))
                .toList();

        return new CursorPageResponse<>(responseList, nextCursor, hasNext);
    }

    /**
     * 공지사항 메타데이터 조회 (OG 태그용)
     */
    public NoticeMetaResponse getNoticeMeta(Long noticeId) {
        Notice notice = noticeRepository
                .findById(noticeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOTICE_NOT_FOUND));

        String description = notice.getContent();
        if (description != null && description.length() > 100) {
            description = description.substring(0, 100);
        }

        List<NoticeImage> thumbnails = noticeImageRepository.findThumbnailsByNoticeIds(List.of(noticeId));
        String thumbnailUrl = thumbnails.isEmpty() ? null : thumbnails.get(0).getImgUrl();

        return new NoticeMetaResponse(notice.getTitle(), description, thumbnailUrl);
    }

    /**
     * 공지사항 상세 조회
     * @param noticeId 공지사항 ID
     * @param userIdentifier 사용자 식별자 (이메일 또는 IP 주소)
     */
    @Transactional
    public NoticeResponse getNoticeDetail(Long noticeId, String userIdentifier) {
        Notice notice = noticeRepository
                .findById(noticeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOTICE_NOT_FOUND));

        // Redis를 사용하여 5분마다만 조회수 증가
        noticeViewRedisRepository.initializeCount(noticeId, notice.getViewCount());
        boolean newViewer = noticeViewRedisRepository.addViewer(noticeId, userIdentifier);
        if (newViewer) {
            noticeViewRedisRepository.incrementCount(noticeId);
        }
        noticeViewRedisRepository.refreshViewers(noticeId);
        Long currentViewCount = noticeViewRedisRepository
                .findCount(noticeId)
                .orElse(notice.getViewCount().longValue());

        // DB 조회수 동기화 (Redis 조회수 읽은 후 실행하여 stale 엔티티 문제 방지)
        if (newViewer) {
            noticeRepository.incrementViewCount(noticeId);
        }

        List<NoticeImage> images = noticeImageRepository.findByNotice(notice);
        int commentCount = (int) noticeCommentRepository.countByNoticeNoticeId(noticeId);
        NoticeResponse response = noticeConverter.toResponseDTO(notice, images, commentCount);
        // Redis 조회수로 덮어쓰기
        response.setViewCount(currentViewCount.intValue());

        // 좋아요 여부 확인 (로그인 사용자만)
        boolean likeStatus = false;
        if (userIdentifier != null && userIdentifier.contains("@")) {
            likeStatus = userRepository
                    .findByEmail(userIdentifier)
                    .map(user -> noticeLikeRepository
                            .findByUserAndNotice(user, notice)
                            .isPresent())
                    .orElse(false);
        }
        response.setLikeStatus(likeStatus);

        return response;
    }

    @Cacheable(cacheNames = "hotNoticeIds")
    public Set<Long> getHotNoticeIds() {
        return new HashSet<>(noticeRepository.findHotNoticeIds(3));
    }

    /**
     * FULLTEXT BOOLEAN MODE 특수문자 제거
     */
    private String sanitizeFulltextKeyword(String keyword) {
        return keyword.trim().replaceAll("[+\\-*~\"()<>@]", " ").trim();
    }
}
