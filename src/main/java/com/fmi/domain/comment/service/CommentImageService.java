package com.fmi.domain.comment.service;

import com.fmi.domain.comment.converter.CommentConverter;
import com.fmi.domain.comment.converter.CommentImageConverter;
import com.fmi.domain.comment.data.Comment;
import com.fmi.domain.comment.data.CommentImage;
import com.fmi.domain.comment.repository.CommentImageRepository;
import com.fmi.domain.comment.web.dto.response.CommentImageResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentImageService {
    private final S3Service s3Service;
    private final CommentImageRepository commentImageRepository;

    @Transactional
    public List<CommentImageResponse> createCommentImageAtS3AndDB(List<MultipartFile> imageList, Comment comment) {
        if (Objects.isNull(imageList) || imageList.isEmpty()) {
            return List.of();
        }

        List<String> uploadImageUrl = s3Service.upload(imageList);

        List<CommentImage> commentImageList = uploadImageUrl.stream()
                .map(imageUrl -> CommentImage.create(imageUrl, comment)).toList();

        commentImageList = commentImageRepository.saveAll(commentImageList);

        return CommentImageConverter.toCommentImageResponseList(commentImageList);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<CommentImageResponse>> buildImageMap(List<Long> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return Map.of();
        }

        List<CommentImage> images = commentImageRepository.findByComment_IdIn(commentIds);

        return images.stream()
                .collect(Collectors.groupingBy(
                        img -> img.getComment().getId(),
                        Collectors.mapping(
                                img -> new CommentImageResponse(
                                        img.getId(),
                                        img.getImgUrl()
                                ),
                                Collectors.toList()
                        )
                ));
    }

    @Transactional(readOnly = true)
    public List<CommentImageResponse> findCommentImageListByComment(Comment comment) {
        List<CommentImage> commentImageList = commentImageRepository.findByComment(comment);

        return CommentImageConverter.toCommentImageResponseList(commentImageList);
    }

    @Transactional
    public void deleteCommentImageAtS3AndDBByImageIdList(List<Long> imageIdList, Long commentId) {
        if (Objects.isNull(imageIdList) || imageIdList.isEmpty()) return;

        List<CommentImage> commentImageList =
                commentImageRepository.findByIdInAndComment_Id(imageIdList, commentId);

        if (commentImageList.size() != imageIdList.size()) {
            throw new GeneralException(ErrorStatus._COMMENT_ACCESS_DENIED);
        }

        List<String> urlList = commentImageList.stream().map(CommentImage::getImgUrl).toList();

        s3Service.delete(urlList);

        commentImageRepository.deleteAllInBatch(commentImageList);
    }

    @Transactional
    public void deleteAllCommentImage(Comment comment) {
        List<CommentImage> images = commentImageRepository.findByComment(comment);

        if (Objects.isNull(images) || images.isEmpty()) {
            return;
        }
        List<String> urls = images.stream().map(CommentImage::getImgUrl).toList();

        commentImageRepository.deleteAllByComment(comment);
        s3Service.delete(urls);
    }
}
