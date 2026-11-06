package com.fmi.domain.admin.service;

import com.fmi.domain.admin.web.dto.response.AdminGuestInquiryPageResponse;
import com.fmi.domain.admin.web.dto.response.AdminInquiryDetailResponse;
import com.fmi.domain.admin.web.dto.response.AdminInquiryResponse;
import com.fmi.domain.inquiry.comment.service.InquiryCommentService;
import com.fmi.domain.inquiry.comment.web.dto.response.InquiryCommentResponse;
import com.fmi.domain.inquiry.inquiry.converter.InquiryConverter;
import com.fmi.domain.inquiry.inquiry.data.Inquiry;
import com.fmi.domain.inquiry.inquiry.data.InquiryImage;
import com.fmi.domain.inquiry.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.inquiry.data.enums.InquiryType;
import com.fmi.domain.inquiry.inquiry.repository.AdminInquiryRepository;
import com.fmi.domain.inquiry.inquiry.repository.InquiryImageRepository;
import com.fmi.domain.inquiry.inquiry.repository.InquiryRepository;
import com.fmi.domain.inquiry.inquiry.web.dto.response.InquiryDetailResponse;
import com.fmi.domain.ipblock.service.IpBlacklistService;
import com.fmi.global.apiPayload.CursorPageResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminInquiryService {
    private final AdminInquiryRepository adminInquiryRepository;

    private final InquiryRepository inquiryRepository;

    private final InquiryImageRepository inquiryImageRepository;

    private final InquiryConverter inquiryConverter;

    private final InquiryCommentService inquiryCommentService;

    private final IpBlacklistService ipBlacklistService;

    public Page<AdminInquiryResponse> getInquiryPage(
            InquiryType type, InquiryStatus status, String keyword, Pageable pageable) {
        Page<Inquiry> inquiries;
        if (keyword != null && !keyword.isBlank()) {
            String typeStr = type != null ? type.name() : null;
            String statusStr = status != null ? status.name() : null;
            Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            inquiries = adminInquiryRepository.findMemberInquiriesForAdminWithKeyword(
                    typeStr, statusStr, sanitizeFulltextKeyword(keyword), unsorted);
        } else {
            inquiries = adminInquiryRepository.findMemberInquiriesForAdmin(type, status, pageable);
        }
        return inquiries.map(inquiry -> AdminInquiryResponse.builder()
                .inquiryId(inquiry.getId())
                .title(inquiry.getTitle())
                .inquiryType(inquiry.getInquiryType())
                .status(inquiry.getAnswerStatus())
                .createdAt(inquiry.getCreatedAt())
                .userId(inquiry.getUser() != null ? inquiry.getUser().getId() : null)
                .nickname(inquiry.getUser() != null ? inquiry.getUser().getNickname() : null)
                .email(
                        inquiry.getEmail() != null
                                ? inquiry.getEmail()
                                : inquiry.getUser() != null ? inquiry.getUser().getEmail() : null)
                .content(inquiry.getContent())
                .ip(inquiry.getIp())
                .answered(inquiry.getAnswered())
                .build());
    }

    /**
     * 문의 상세 조회 (관리자용 - 비공개 문의도 조회 가능)
     */
    public AdminInquiryDetailResponse getInquiryDetail(Long inquiryId, UserDetails userDetails) {
        Inquiry inquiry = inquiryRepository
                .findById(inquiryId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._INQUIRY_NOT_FOUND));

        List<InquiryCommentResponse> comments =
                inquiryCommentService.getCommentsForDetail(inquiry.getId(), userDetails);
        List<String> imageUrls = inquiryImageRepository.findByInquiryId(inquiry.getId()).stream()
                .map(InquiryImage::getImgUrl)
                .toList();
        return inquiryConverter.toAdminDetailDTO(inquiry, comments, imageUrls);
    }

    public InquiryDetailResponse getGuestInquiryDetail(Long inquiryId) {
        Inquiry inquiry = inquiryRepository
                .findById(inquiryId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._INQUIRY_NOT_FOUND));

        if (inquiry.getUser() != null) {
            throw new GeneralException(ErrorStatus._INQUIRY_NOT_FOUND);
        }

        return inquiryConverter.toDetailDTO(inquiry);
    }

    @Transactional
    public void blockInquiryIp(Long inquiryId, String reason) {
        Inquiry inquiry = inquiryRepository
                .findById(inquiryId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._INQUIRY_NOT_FOUND));
        ipBlacklistService.blockIp(inquiry.getIp(), reason);
    }

    /**
     * 관리자 비회원 문의 목록 조회 (커서 기반 무한스크롤, id 기반)
     */
    public AdminGuestInquiryPageResponse getGuestInquirySlice(
            InquiryStatus status, Boolean answered, String keyword, Long cursor, int size) {
        size = Math.max(1, Math.min(size, 50));

        // answered 파라미터를 status로 변환 (status가 없을 때만)
        InquiryStatus effectiveStatus = status;
        boolean notAnsweredFilter = false;
        if (status == null && answered != null) {
            if (answered) {
                effectiveStatus = InquiryStatus.ANSWERED;
            } else {
                notAnsweredFilter = true;
            }
        }

        PageRequest pageRequest = PageRequest.of(0, size);
        Slice<Inquiry> inquirySlice;

        String sanitizedKeyword = (keyword != null && !keyword.isBlank()) ? sanitizeFulltextKeyword(keyword) : null;
        if (sanitizedKeyword != null) {
            String statusStr = effectiveStatus != null ? effectiveStatus.name() : null;
            inquirySlice = notAnsweredFilter
                    ? adminInquiryRepository.findGuestInquiriesNotAnsweredWithKeywordSlice(
                            sanitizedKeyword, cursor, pageRequest)
                    : adminInquiryRepository.findGuestInquiriesForAdminWithKeywordSlice(
                            statusStr, sanitizedKeyword, cursor, pageRequest);
        } else if (notAnsweredFilter) {
            inquirySlice = (cursor == null)
                    ? adminInquiryRepository.findGuestInquiriesNotAnsweredOrderByIdDesc(pageRequest)
                    : adminInquiryRepository.findGuestInquiriesNotAnsweredBeforeCursorOrderByIdDesc(
                            cursor, pageRequest);
        } else {
            inquirySlice = (cursor == null)
                    ? adminInquiryRepository.findGuestInquiriesOrderByIdDesc(effectiveStatus, pageRequest)
                    : adminInquiryRepository.findGuestInquiriesBeforeCursorOrderByIdDesc(
                            effectiveStatus, cursor, pageRequest);
        }

        List<AdminInquiryResponse> items = inquirySlice.getContent().stream()
                .map(inquiry -> AdminInquiryResponse.builder()
                        .inquiryId(inquiry.getId())
                        .title(inquiry.getTitle())
                        .inquiryType(inquiry.getInquiryType())
                        .status(inquiry.getAnswerStatus())
                        .createdAt(inquiry.getCreatedAt())
                        .userId(null)
                        .nickname(null)
                        .email(inquiry.getEmail())
                        .content(inquiry.getContent())
                        .ip(inquiry.getIp())
                        .answered(inquiry.getAnswered())
                        .build())
                .toList();

        boolean hasNext = inquirySlice.hasNext();
        Long nextCursor =
                (hasNext && !items.isEmpty()) ? items.get(items.size() - 1).getInquiryId() : null;

        return new AdminGuestInquiryPageResponse(items, nextCursor, hasNext);
    }

    /**
     * FULLTEXT BOOLEAN MODE 특수문자 제거
     */
    private String sanitizeFulltextKeyword(String keyword) {
        String sanitized = keyword.trim().replaceAll("[+\\-*~\"()<>@]", " ").trim();
        return sanitized.isEmpty() ? null : sanitized;
    }

    public CursorPageResponse<AdminInquiryResponse> getInquiryCursorPage(
            InquiryType type, InquiryStatus status, Boolean answered, String keyword, Long cursor, int size) {
        int fetchSize = size + 1;

        List<Inquiry> inquiries;
        String sanitizedKeyword = (keyword != null && !keyword.isBlank()) ? sanitizeFulltextKeyword(keyword) : null;
        if (sanitizedKeyword != null) {
            String typeStr = type != null ? type.name() : null;
            String statusStr = status != null ? status.name() : null;
            inquiries = adminInquiryRepository.findAllForAdminWithKeywordCursor(
                    typeStr, statusStr, answered, sanitizedKeyword, cursor, fetchSize);
        } else {
            Pageable limit = PageRequest.of(0, fetchSize);
            inquiries = adminInquiryRepository.findAllForAdminCursor(type, status, answered, cursor, limit);
        }

        boolean hasNext = inquiries.size() > size;
        List<Inquiry> content = hasNext ? inquiries.subList(0, size) : inquiries;
        Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;

        List<AdminInquiryResponse> responseList = content.stream()
                .map(inquiry -> AdminInquiryResponse.builder()
                        .inquiryId(inquiry.getId())
                        .title(inquiry.getTitle())
                        .inquiryType(inquiry.getInquiryType())
                        .status(inquiry.getAnswerStatus())
                        .createdAt(inquiry.getCreatedAt())
                        .userId(inquiry.getUser() != null ? inquiry.getUser().getId() : null)
                        .nickname(inquiry.getUser() != null ? inquiry.getUser().getNickname() : null)
                        .email(
                                inquiry.getEmail() != null
                                        ? inquiry.getEmail()
                                        : inquiry.getUser() != null
                                                ? inquiry.getUser().getEmail()
                                                : null)
                        .content(inquiry.getContent())
                        .ip(inquiry.getIp())
                        .answered(inquiry.getAnswered())
                        .build())
                .toList();

        return new CursorPageResponse<>(responseList, nextCursor, hasNext);
    }
}
