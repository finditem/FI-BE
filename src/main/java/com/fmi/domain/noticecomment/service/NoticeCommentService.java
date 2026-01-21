package com.fmi.domain.noticecomment.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.notice.data.Notice;
import com.fmi.domain.notice.repository.NoticeRepository;
import com.fmi.domain.noticecomment.converter.NoticeCommentConverter;
import com.fmi.domain.noticecomment.data.NoticeComment;
import com.fmi.domain.noticecomment.repository.NoticeCommentRepository;
import com.fmi.domain.noticecomment.response.NoticeCommentResponse;
import com.fmi.domain.noticecomment.response.NoticeCommentSliceResponse;
import com.fmi.domain.noticecomment.web.dto.CreateNoticeCommentDto;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeCommentService {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final NoticeCommentRepository noticeCommentRepository;
    private final NoticeCommentConverter noticeCommentConverter;

    @Transactional
    public NoticeCommentResponse createComment(Long noticeId, CreateNoticeCommentDto dto, UserDetails userDetails) {
        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._NOTICE_NOT_FOUND));

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        NoticeComment parent = null;
        if (dto.getParentId() != null) {
            parent = noticeCommentRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_NOT_FOUND));
            if (!parent.getNotice().getNoticeId().equals(noticeId)) {
                throw new GeneralException(ErrorStatus._BAD_REQUEST);
            }
        }

        NoticeComment comment = noticeCommentConverter.toEntity(dto, user, notice, parent);
        NoticeComment saved = noticeCommentRepository.save(comment);
        return noticeCommentConverter.toResponse(saved);
    }

    public NoticeCommentSliceResponse getComments(Long noticeId, Long cursor, int size) {
        if (!noticeRepository.existsById(noticeId)) {
            throw new GeneralException(ErrorStatus._NOTICE_NOT_FOUND);
        }

        Slice<NoticeComment> comments;
        if (cursor == null) {
            comments = noticeCommentRepository.findTopByNoticeIdOrderByIdDesc(noticeId, Pageable.ofSize(size));
        } else {
            comments = noticeCommentRepository.findByNoticeIdAndIdLessThanOrderByIdDesc(noticeId, cursor, Pageable.ofSize(size));
        }

        Long nextCursor = comments.hasNext()
                ? comments.getContent().get(comments.getContent().size() - 1).getId()
                : null;

        List<NoticeCommentResponse> result = comments.stream()
                .limit(size)
                .map(noticeCommentConverter::toResponse)
                .toList();

        return new NoticeCommentSliceResponse(result, comments.hasNext(), nextCursor);
    }

    @Transactional
    public NoticeCommentResponse updateComment(Long commentId, CreateNoticeCommentDto dto, UserDetails userDetails) {
        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        NoticeComment comment = noticeCommentRepository.findById(commentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_NOT_FOUND));

        if (!comment.getUser().getEmail().equals(userDetails.getUsername())) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }

        comment.updateContent(dto.getContent());
        return noticeCommentConverter.toResponse(comment);
    }

    @Transactional
    public NoticeCommentResponse deleteComment(Long commentId, UserDetails userDetails) {
        if (userDetails == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }

        NoticeComment comment = noticeCommentRepository.findById(commentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._COMMENT_NOT_FOUND));

        boolean isOwner = comment.getUser().getEmail().equals(userDetails.getUsername());
        boolean isAdmin = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }

        noticeCommentRepository.delete(comment);
        return noticeCommentConverter.toResponse(comment);
    }
}
