package com.fmi.domain.admin.service;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.Enum.WithdrawalReason;
import com.fmi.domain.admin.dto.AdminDeletedUserResponse;
import com.fmi.domain.admin.dto.AdminInquiryResponse;
import com.fmi.domain.admin.dto.AdminReportResponse;
import com.fmi.domain.admin.dto.AdminUserDetailResponse;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.comment.repository.CommentRepository;
import com.fmi.domain.inquiry.data.Inquiry;
import com.fmi.domain.inquiry.data.InquiryImage;
import com.fmi.domain.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.data.enums.InquiryType;
import com.fmi.domain.inquiry.repository.InquiryImageRepository;
import com.fmi.domain.inquiry.repository.InquiryRepository;
import com.fmi.domain.ipblock.service.IpBlacklistService;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.report.data.Report;
import com.fmi.domain.report.data.ReportAnswerImage;
import com.fmi.domain.report.data.enums.ReportStatus;
import com.fmi.domain.report.data.enums.ReportTargetType;
import com.fmi.domain.report.repository.ReportAnswerImageRepository;
import com.fmi.domain.report.repository.ReportRepository;
import com.fmi.domain.inquiry.converter.InquiryConverter;
import com.fmi.domain.admin.dto.AdminInquiryDetailDTO;
import com.fmi.domain.inquiry.web.dto.response.InquiryDetailDTO;
import com.fmi.domain.inquirycomment.response.InquiryCommentResponse;
import com.fmi.domain.inquirycomment.service.InquiryCommentService;
import com.fmi.domain.report.converter.ReportConverter;
import com.fmi.domain.report.service.ReportService;
import com.fmi.domain.report.web.dto.response.ReportResponseDTO;
import com.fmi.domain.user.data.UserCategory;
import com.fmi.domain.user.repository.UserCategoryRepository;
import com.fmi.global.apiPayload.CursorPageResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fmi.domain.admin.dto.AdminGuestInquiryPageResponse;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final InquiryRepository inquiryRepository;
    private final InquiryImageRepository inquiryImageRepository;
    private final InquiryConverter inquiryConverter;
    private final ReportRepository reportRepository;
    private final ReportConverter reportConverter;
    private final ReportService reportService;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserCategoryRepository userCategoryRepository;
    private final InquiryCommentService inquiryCommentService;
    private final IpBlacklistService ipBlacklistService;
    private final ReportAnswerImageRepository reportAnswerImageRepository;

    public Page<AdminInquiryResponse> getInquiryPage(InquiryType type,
                                                     InquiryStatus status,
                                                     String keyword,
                                                     Pageable pageable) {
        Page<Inquiry> inquiries;
        if (keyword != null && !keyword.isBlank()) {
            String typeStr = type != null ? type.name() : null;
            String statusStr = status != null ? status.name() : null;
            Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            inquiries = inquiryRepository.findMemberInquiriesForAdminWithKeyword(typeStr, statusStr, sanitizeFulltextKeyword(keyword), unsorted);
        } else {
            inquiries = inquiryRepository.findMemberInquiriesForAdmin(type, status, pageable);
        }
        return inquiries.map(inquiry -> AdminInquiryResponse.builder()
                .inquiryId(inquiry.getId())
                .title(inquiry.getTitle())
                .inquiryType(inquiry.getInquiryType())
                .status(inquiry.getAnswerStatus())
                .createdAt(inquiry.getCreatedAt())
                .userId(inquiry.getUser() != null ? inquiry.getUser().getId() : null)
                .nickname(inquiry.getUser() != null ? inquiry.getUser().getNickname() : null)
                .email(inquiry.getEmail() != null ? inquiry.getEmail() :
                        inquiry.getUser() != null ? inquiry.getUser().getEmail() : null)
                .content(inquiry.getContent())
                .ip(inquiry.getIp())
                .answered(inquiry.getAnswered())
                .build());
    }

    public Page<AdminReportResponse> getReportPage(ReportStatus status,
                                                   ReportTargetType targetType,
                                                   Boolean answered,
                                                   String keyword,
                                                   Pageable pageable) {
        Page<Report> reports;
        if (keyword != null && !keyword.isBlank()) {
            String statusStr = status != null ? status.name() : null;
            String targetTypeStr = targetType != null ? targetType.name() : null;
            Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            reports = reportRepository.findAllForAdminWithKeyword(statusStr, targetTypeStr, answered, sanitizeFulltextKeyword(keyword), unsorted);
        } else {
            reports = reportRepository.findAllForAdmin(status, targetType, answered, pageable);
        }
        return reports.map(report -> AdminReportResponse.builder()
                .reportId(report.getReportId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reportType(report.getReportType())
                .status(report.getStatus())
                .reason(report.getReason())
                .targetTitle(reportService.getTargetTitle(report.getTargetType(), report.getTargetId()))
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .resolvedAt(report.getResolvedAt())
                .reporterId(report.getReporter() != null ? report.getReporter().getId() : null)
                .reporterNickname(report.getReporter() != null ? report.getReporter().getNickname() : null)
                .reporterEmail(report.getReporter() != null ? report.getReporter().getEmail() : null)
                .answered(report.getAnswered())
                .build());
    }

    public AdminUserDetailResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        long postCount = postRepository.countByUserAndDeletedFalse(user);
        long commentCount = commentRepository.countByUser(user);
        long reportCount = reportRepository.countByReporter(user);
        List<Category> categories = userCategoryRepository.findAllByUser(user).stream()
                .map(UserCategory::getCategory)
                .toList();

        return AdminUserDetailResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .emailVerified(user.isEmail_verified())
                .role(user.getRole())
                .profileImg(user.getProfile_img())
                .privacyPolicyAgreed(user.isPrivacyPolicyAgreed())
                .termsOfServiceAgreed(user.isTermsOfServiceAgreed())
                .contentPolicyAgreed(user.isContentPolicyAgreed())
                .marketingConsent(user.isMarketingConsent())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt())
                .postCount(postCount)
                .commentCount(commentCount)
                .reportCount(reportCount)
                .subscribedCategories(categories)
                .build();
    }

    /**
     * 문의 상세 조회 (관리자용 - 비공개 문의도 조회 가능)
     */
    public AdminInquiryDetailDTO getInquiryDetail(Long inquiryId, UserDetails userDetails) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._INQUIRY_NOT_FOUND));

        List<InquiryCommentResponse> comments =
                inquiryCommentService.getCommentsForDetail(inquiry.getId(), userDetails);
        List<String> imageUrls = inquiryImageRepository.findByInquiryId(inquiry.getId()).stream()
                .map(InquiryImage::getImgUrl)
                .toList();
        return inquiryConverter.toAdminDetailDTO(inquiry, comments, imageUrls);
    }

    public InquiryDetailDTO getGuestInquiryDetail(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._INQUIRY_NOT_FOUND));

        if (inquiry.getUser() != null) {
            throw new GeneralException(ErrorStatus._INQUIRY_NOT_FOUND);
        }

        return inquiryConverter.toDetailDTO(inquiry);
    }

    @Transactional
    public void blockInquiryIp(Long inquiryId, String reason) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._INQUIRY_NOT_FOUND));
        ipBlacklistService.blockIp(inquiry.getIp(), reason);
    }

    /**
     * 신고 상세 조회 (관리자용)
     */
    public ReportResponseDTO getReportDetail(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._REPORT_NOT_FOUND));

        String targetTitle = reportService.getTargetTitle(report.getTargetType(), report.getTargetId());
        List<String> answerImageUrls = reportAnswerImageRepository.findByReportReportId(reportId).stream()
                .map(ReportAnswerImage::getImageUrl)
                .toList();
        return reportConverter.toResponseDTO(report, targetTitle, answerImageUrls);
    }

    /**
     * 관리자 비회원 문의 목록 조회 (커서 기반 무한스크롤, id 기반)
     */
    public AdminGuestInquiryPageResponse getGuestInquirySlice(InquiryStatus status,
                                                              Boolean answered,
                                                              String keyword,
                                                              Long cursor,
                                                              int size) {
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
                    ? inquiryRepository.findGuestInquiriesNotAnsweredWithKeywordSlice(
                            sanitizedKeyword, cursor, pageRequest)
                    : inquiryRepository.findGuestInquiriesForAdminWithKeywordSlice(
                            statusStr, sanitizedKeyword, cursor, pageRequest);
        } else if (notAnsweredFilter) {
            inquirySlice = (cursor == null)
                    ? inquiryRepository.findGuestInquiriesNotAnsweredOrderByIdDesc(pageRequest)
                    : inquiryRepository.findGuestInquiriesNotAnsweredBeforeCursorOrderByIdDesc(cursor, pageRequest);
        } else {
            inquirySlice = (cursor == null)
                    ? inquiryRepository.findGuestInquiriesOrderByIdDesc(effectiveStatus, pageRequest)
                    : inquiryRepository.findGuestInquiriesBeforeCursorOrderByIdDesc(effectiveStatus, cursor, pageRequest);
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
        Long nextCursor = (hasNext && !items.isEmpty())
                ? items.get(items.size() - 1).getInquiryId()
                : null;

        return new AdminGuestInquiryPageResponse(items, nextCursor, hasNext);
    }

    /**
     * FULLTEXT BOOLEAN MODE 특수문자 제거
     */
    private String sanitizeFulltextKeyword(String keyword) {
        String sanitized = keyword.trim().replaceAll("[+\\-*~\"()<>@]", " ").trim();
        return sanitized.isEmpty() ? null : sanitized;
    }

    /**
     * 탈퇴 유저 목록 조회
     */
    public Page<AdminDeletedUserResponse> getDeletedUsers(WithdrawalReason reason, Pageable pageable) {
        String reasonStr = reason != null ? reason.name() : null;
        Page<User> deletedUsers = userRepository.findDeletedUsers(reasonStr, pageable);

        return deletedUsers.map(user -> AdminDeletedUserResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .deletedAt(user.getDeletedAt())
                .withdrawalReason(user.getWithdrawalReason())
                .withdrawalOtherReason(user.getWithdrawalOtherReason())
                .build());
    }

    public CursorPageResponse<AdminInquiryResponse> getInquiryCursorPage(
            InquiryType type, InquiryStatus status, Boolean answered, String keyword, Long cursor, int size) {
        int fetchSize = size + 1;

        List<Inquiry> inquiries;
        String sanitizedKeyword = (keyword != null && !keyword.isBlank()) ? sanitizeFulltextKeyword(keyword) : null;
        if (sanitizedKeyword != null) {
            String typeStr = type != null ? type.name() : null;
            String statusStr = status != null ? status.name() : null;
            inquiries = inquiryRepository.findAllForAdminWithKeywordCursor(
                    typeStr, statusStr, answered, sanitizedKeyword, cursor, fetchSize);
        } else {
            Pageable limit = PageRequest.of(0, fetchSize);
            inquiries = inquiryRepository.findAllForAdminCursor(type, status, answered, cursor, limit);
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
                        .email(inquiry.getEmail() != null ? inquiry.getEmail() :
                                inquiry.getUser() != null ? inquiry.getUser().getEmail() : null)
                        .content(inquiry.getContent())
                        .ip(inquiry.getIp())
                        .answered(inquiry.getAnswered())
                        .build())
                .toList();

        return new CursorPageResponse<>(responseList, nextCursor, hasNext);
    }

    public CursorPageResponse<AdminReportResponse> getReportCursorPage(
            ReportStatus status, ReportTargetType targetType, Boolean answered,
            String keyword, Long cursor, int size) {
        int fetchSize = size + 1;
        List<Report> reports;
        String sanitizedKeyword = (keyword != null && !keyword.isBlank()) ? sanitizeFulltextKeyword(keyword) : null;
        if (sanitizedKeyword != null) {
            String statusStr = status != null ? status.name() : null;
            String targetTypeStr = targetType != null ? targetType.name() : null;
            reports = reportRepository.findAllForAdminWithKeywordCursor(
                    statusStr, targetTypeStr, answered, sanitizedKeyword, cursor, fetchSize);
        } else {
            Pageable limit = PageRequest.of(0, fetchSize);
            reports = reportRepository.findAllForAdminCursor(status, targetType, answered, cursor, limit);
        }

        boolean hasNext = reports.size() > size;
        List<Report> content = hasNext ? reports.subList(0, size) : reports;
        Long nextCursor = hasNext ? content.get(content.size() - 1).getReportId() : null;

        List<AdminReportResponse> responseList = content.stream()
                .map(report -> AdminReportResponse.builder()
                        .reportId(report.getReportId())
                        .targetType(report.getTargetType())
                        .targetId(report.getTargetId())
                        .reportType(report.getReportType())
                        .status(report.getStatus())
                        .reason(report.getReason())
                        .targetTitle(reportService.getTargetTitle(report.getTargetType(), report.getTargetId()))
                        .createdAt(report.getCreatedAt())
                        .updatedAt(report.getUpdatedAt())
                        .resolvedAt(report.getResolvedAt())
                        .reporterId(report.getReporter() != null ? report.getReporter().getId() : null)
                        .reporterNickname(report.getReporter() != null ? report.getReporter().getNickname() : null)
                        .reporterEmail(report.getReporter() != null ? report.getReporter().getEmail() : null)
                        .answered(report.getAnswered())
                        .build())
                .toList();

        return new CursorPageResponse<>(responseList, nextCursor, hasNext);
    }

    public CursorPageResponse<AdminDeletedUserResponse> getDeletedUsersCursorPage(
            WithdrawalReason reason, String keyword, Long cursor, int size) {
        int fetchSize = size + 1;
        String reasonStr = reason != null ? reason.name() : null;
        String keywordParam = (keyword != null && !keyword.isBlank()) ? "%" + keyword.trim() + "%" : null;
        List<User> users = userRepository.findDeletedUsersCursorWithKeyword(reasonStr, keywordParam, cursor, fetchSize);

        boolean hasNext = users.size() > size;
        List<User> content = hasNext ? users.subList(0, size) : users;
        Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;

        List<AdminDeletedUserResponse> responseList = content.stream()
                .map(user -> AdminDeletedUserResponse.builder()
                        .userId(user.getId())
                        .nickname(user.getNickname())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .createdAt(user.getCreatedAt())
                        .deletedAt(user.getDeletedAt())
                        .withdrawalReason(user.getWithdrawalReason())
                        .withdrawalOtherReason(user.getWithdrawalOtherReason())
                        .build())
                .toList();

        return new CursorPageResponse<>(responseList, nextCursor, hasNext);
    }
}

