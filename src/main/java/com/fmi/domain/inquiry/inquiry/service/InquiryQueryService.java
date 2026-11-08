package com.fmi.domain.inquiry.inquiry.service;

import com.fmi.domain.inquiry.comment.service.InquiryCommentService;
import com.fmi.domain.inquiry.comment.web.dto.response.InquiryCommentResponse;
import com.fmi.domain.inquiry.inquiry.converter.InquiryConverter;
import com.fmi.domain.inquiry.inquiry.data.Inquiry;
import com.fmi.domain.inquiry.inquiry.data.InquiryImage;
import com.fmi.domain.inquiry.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.inquiry.repository.InquiryImageRepository;
import com.fmi.domain.inquiry.inquiry.repository.InquiryRepository;
import com.fmi.domain.inquiry.inquiry.web.dto.response.InquiryDetailResponse;
import com.fmi.domain.inquiry.inquiry.web.dto.response.InquiryListResponse;
import com.fmi.domain.user.user.data.User;
import com.fmi.domain.user.user.repository.UserRepository;
import com.fmi.global.apiPayload.CursorPageResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryQueryService {

    private final InquiryRepository inquiryRepository;

    private final InquiryImageRepository inquiryImageRepository;

    private final InquiryConverter inquiryConverter;

    private final UserRepository userRepository;

    private final InquiryCommentService inquiryCommentService;

    /**
     * 내 문의 내역 조회
     */
    public Page<InquiryListResponse> getMyInquiries(
            User user, InquiryStatus status, Boolean answered, Pageable pageable) {
        Page<Inquiry> inquiries;

        if (status != null) {
            inquiries = inquiryRepository.findByUserAndAnswerStatus(user, status, pageable);
        } else if (answered != null) {
            if (answered) {
                inquiries = inquiryRepository.findByUserAndAnswerStatus(user, InquiryStatus.ANSWERED, pageable);
            } else {
                inquiries = inquiryRepository.findByUserAndAnswerStatusNot(user, InquiryStatus.ANSWERED, pageable);
            }
        } else {
            inquiries = inquiryRepository.findByUser(user, pageable);
        }

        return inquiries.map(inquiry -> inquiryConverter.toListDTO(inquiry));
    }

    /**
     * 내 문의 내역 조회 (커서 기반)
     */
    public CursorPageResponse<InquiryListResponse> getMyInquiriesCursor(
            User user, InquiryStatus status, Boolean answered, String keyword, Long cursor, int size) {
        int fetchSize = size + 1;
        Pageable limit = PageRequest.of(0, fetchSize);
        List<Inquiry> inquiries;

        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        if (trimmedKeyword != null) {
            if (status != null) {
                inquiries = inquiryRepository.findByUserAndAnswerStatusAndKeywordCursor(
                        user, status, trimmedKeyword, cursor, limit);
            } else if (answered != null) {
                if (answered) {
                    inquiries = inquiryRepository.findByUserAndAnswerStatusAndKeywordCursor(
                            user, InquiryStatus.ANSWERED, trimmedKeyword, cursor, limit);
                } else {
                    inquiries = inquiryRepository.findByUserAndAnswerStatusNotAndKeywordCursor(
                            user, InquiryStatus.ANSWERED, trimmedKeyword, cursor, limit);
                }
            } else {
                inquiries = inquiryRepository.findByUserAndKeywordCursor(user, trimmedKeyword, cursor, limit);
            }
        } else {
            if (status != null) {
                inquiries = inquiryRepository.findByUserAndAnswerStatusCursor(user, status, cursor, limit);
            } else if (answered != null) {
                if (answered) {
                    inquiries = inquiryRepository.findByUserAndAnswerStatusCursor(
                            user, InquiryStatus.ANSWERED, cursor, limit);
                } else {
                    inquiries = inquiryRepository.findByUserAndAnswerStatusNotCursor(
                            user, InquiryStatus.ANSWERED, cursor, limit);
                }
            } else {
                inquiries = inquiryRepository.findByUserCursor(user, cursor, limit);
            }
        }

        boolean hasNext = inquiries.size() > size;
        List<Inquiry> content = hasNext ? inquiries.subList(0, size) : inquiries;
        Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;

        List<InquiryListResponse> responseList =
                content.stream().map(inquiryConverter::toListDTO).toList();

        return new CursorPageResponse<>(responseList, nextCursor, hasNext);
    }

    /**
     * 문의 상세 조회
     * - 회원 문의: user로 확인
     * - 관리자: 항상 접근 가능
     */
    public InquiryDetailResponse getInquiryDetail(Long inquiryId, UserDetails userDetails) {
        Inquiry inquiry = inquiryRepository
                .findById(inquiryId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._INQUIRY_NOT_FOUND));

        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        User user = userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        // 권한 확인: 관리자는 모든 문의 조회 가능, 일반 사용자는 본인 문의만
        if (!isAdmin(userDetails)) {
            if (inquiry.getUser() != null) {
                if (!inquiry.getUser().getId().equals(user.getId())) {
                    throw new GeneralException(ErrorStatus._INQUIRY_ACCESS_DENIED);
                }
            } else {
                // 비회원 문의는 관리자만 조회 가능
                throw new GeneralException(ErrorStatus._INQUIRY_ACCESS_DENIED);
            }
        }

        java.util.List<InquiryCommentResponse> comments =
                inquiryCommentService.getCommentsForDetail(inquiry.getId(), userDetails);
        List<String> imageUrls = inquiryImageRepository.findByInquiryId(inquiry.getId()).stream()
                .map(InquiryImage::getImgUrl)
                .toList();
        return inquiryConverter.toDetailDTO(inquiry, comments, imageUrls);
    }

    private boolean isAdmin(UserDetails userDetails) {
        if (userDetails == null) {
            return false;
        }
        return userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN"));
    }
}
