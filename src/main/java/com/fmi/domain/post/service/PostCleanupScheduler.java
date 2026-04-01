package com.fmi.domain.post.service;

import com.fmi.domain.comment.data.CommentImage;
import com.fmi.domain.comment.repository.CommentImageRepository;
import com.fmi.domain.comment.repository.CommentRepository;
import com.fmi.domain.commentlike.repository.CommentLikeRepository;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostImage;
import com.fmi.domain.post.repository.PostImageRepository;
import com.fmi.domain.post.repository.PostRepository;
import com.fmi.domain.postfavorite.repository.PostFavoriteRepository;
import com.fmi.global.service.S3Service;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostCleanupScheduler {
    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostFavoriteRepository postFavoriteRepository;
    private final CommentImageRepository commentImageRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;
    private final S3Service s3Service;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupDeletedPosts() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);

        List<Post> expiredPosts = postRepository.findExpiredDeletedPosts(threshold);

        if (expiredPosts.isEmpty()) {
            log.info("삭제 만료된 게시글이 없습니다.");
            return;
        }

        List<Long> postIdList = expiredPosts.stream()
                .map(Post::getId).
                toList();

        List<PostImage> postImageList = postImageRepository.findAllByPostIds(postIdList);

        List<String> imageUrlList = postImageList.stream()
                .map(PostImage::getImgUrl)
                .filter(url -> Objects.nonNull(url) && !url.isBlank())
                .toList();

        List<CommentImage> commentImages = commentImageRepository.findAllByPostIds(postIdList);
        List<String> commentImageUrls = commentImages.stream()
                .map(CommentImage::getImgUrl)
                .filter(url -> url != null && !url.isBlank())
                .toList();

        try {
            if (!imageUrlList.isEmpty()) {
                s3Service.delete(imageUrlList);
            }

            if (!commentImageUrls.isEmpty()) {
                s3Service.delete(commentImageUrls);
            }
        } catch (Exception e) {
            log.error("s3 이미지 삭제 실패");
        }

        commentImageRepository.deleteAllByPostIds(postIdList);
        commentLikeRepository.deleteAllByPostIds(postIdList);
        commentRepository.deleteAllByPostIds(postIdList);

        postImageRepository.deleteAllByPostIds(postIdList);
        postFavoriteRepository.deleteAllByPostIds(postIdList);
        postRepository.deleteAllInBatch(expiredPosts);

        log.info("삭제 만료 게시글 정리 완료, postCount={}", postIdList.size());

    }
}
