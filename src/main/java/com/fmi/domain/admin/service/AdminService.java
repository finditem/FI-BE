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
import com.fmi.domain.inquiry.data.enums.InquiryCategory;
import com.fmi.domain.inquiry.data.enums.InquiryStatus;
import com.fmi.domain.inquiry.data.enums.InquiryType;
import com.fmi.domain.inquiry.repository.InquiryReplyRepository;
import com.fmi.domain.inquiry.repository.InquiryRepository;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.report.data.Report;
import com.fmi.domain.report.data.enums.ReportStatus;
import com.fmi.domain.report.data.enums.ReportTargetType;
import com.fmi.domain.report.repository.ReportRepository;
import com.fmi.domain.inquiry.converter.InquiryConverter;
import com.fmi.domain.inquiry.data.InquiryReply;
import com.fmi.domain.inquiry.web.dto.response.InquiryDetailDTO;
import com.fmi.domain.report.converter.ReportConverter;
import com.fmi.domain.report.web.dto.response.ReportResponseDTO;
import com.fmi.domain.user.data.UserCategory;
import com.fmi.domain.user.repository.UserCategoryRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final InquiryRepository inquiryRepository;
    private final InquiryReplyRepository inquiryReplyRepository;
    private final InquiryConverter inquiryConverter;
    private final ReportRepository reportRepository;
    private final ReportConverter reportConverter;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserCategoryRepository userCategoryRepository;

    public Page<AdminInquiryResponse> getInquiryPage(InquiryType type,
                                                     InquiryStatus status,
                                                     InquiryCategory category,
                                                     Pageable pageable) {
        Page<Inquiry> inquiries = inquiryRepository.findAllForAdmin(type, status, category, pageable);
        return inquiries.map(inquiry -> AdminInquiryResponse.builder()
                .inquiryId(inquiry.getId())
                .title(inquiry.getTitle())
                .inquiryType(inquiry.getInquiryType())
                .category(inquiry.getCategory())
                .status(inquiry.getAnswerStatus())
                .createdAt(inquiry.getCreatedAt())
                .userId(inquiry.getUser() != null ? inquiry.getUser().getId() : null)
                .userNickname(inquiry.getUser() != null ? inquiry.getUser().getNickname() : null)
                .userEmail(inquiry.getEmail() != null ? inquiry.getEmail() :
                        inquiry.getUser() != null ? inquiry.getUser().getEmail() : null)
                .build());
    }

    public Page<AdminReportResponse> getReportPage(ReportStatus status,
                                                   ReportTargetType targetType,
                                                   Pageable pageable) {
        Page<Report> reports = reportRepository.findAllForAdmin(status, targetType, pageable);
        return reports.map(report -> AdminReportResponse.builder()
                .reportId(report.getReportId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reportType(report.getReportType())
                .status(report.getStatus())
                .reason(report.getReason())
                .adminNote(report.getAdminNote())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .resolvedAt(report.getResolvedAt())
                .reporterId(report.getReporter() != null ? report.getReporter().getId() : null)
                .reporterNickname(report.getReporter() != null ? report.getReporter().getNickname() : null)
                .reporterEmail(report.getReporter() != null ? report.getReporter().getEmail() : null)
                .build());
    }

    public AdminUserDetailResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        long postCount = postRepository.countByUser(user);
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
                .termsOfServiceAgreed(user.isTermsOfServiceAgreed())
                .privacyPolicyAgreed(user.isPrivacyPolicyAgreed())
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
    public InquiryDetailDTO getInquiryDetail(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._INQUIRY_NOT_FOUND));
        
        // 답변 조회
        InquiryReply reply = inquiryReplyRepository.findByInquiry(inquiry).orElse(null);
        
        return inquiryConverter.toDetailDTO(inquiry, reply);
    }

    /**
     * 신고 상세 조회 (관리자용)
     */
    public ReportResponseDTO getReportDetail(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._REPORT_NOT_FOUND));
        
        return reportConverter.toResponseDTO(report);
    }

    /**
     * 탈퇴 유저 목록 조회
     */
    public Page<AdminDeletedUserResponse> getDeletedUsers(WithdrawalReason reason, Pageable pageable) {
        Page<User> deletedUsers = userRepository.findDeletedUsers(reason, pageable);
        
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
}

