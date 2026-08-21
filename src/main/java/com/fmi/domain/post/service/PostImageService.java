package com.fmi.domain.post.service;

import com.fmi.domain.post.converter.util.PostImageConverter;
import com.fmi.domain.post.data.ImageType;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostImage;
import com.fmi.domain.post.repository.PostImageRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import com.fmi.global.service.S3Service;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PostImageService {
    private final S3Service s3Service;
    private final PostImageRepository postImageRepository;

    public void applyThumbNail(List<MultipartFile> imageList, Post post, Long thumbnailId, List<Long> keepImageIdList) {
        postImageRepository.resetThumbnailToNormal(post.getId());

        PostImage thumb = null;

        if (Objects.nonNull(thumbnailId)) {
            thumb = postImageRepository
                    .findByIdAndPost_Id(thumbnailId, post.getId())
                    .orElse(null);

            if (Objects.nonNull(thumb)
                    && Objects.nonNull(keepImageIdList)
                    && !keepImageIdList.isEmpty()
                    && !keepImageIdList.contains(thumbnailId)) {
                thumb = null;
            }
        }

        List<PostImage> newlySaved = createPostImageNormalAtS3AndDB(imageList, post);

        if (Objects.nonNull(thumb)) {
            thumb.setImageType(ImageType.THUMBNAIL);
            return;
        }

        if (!newlySaved.isEmpty()) {
            newlySaved.get(0).setImageType(ImageType.THUMBNAIL);
            return;
        }

        postImageRepository.flush();

        List<PostImage> remain = postImageRepository.findByPost(post);

        if (Objects.nonNull(keepImageIdList) && !keepImageIdList.isEmpty()) {
            Map<Long, PostImage> remainMap =
                    remain.stream().collect(Collectors.toMap(PostImage::getId, i -> i, (a, b) -> a));

            for (Long keepId : keepImageIdList) {
                PostImage kept = remainMap.get(keepId);
                if (kept != null) {
                    kept.setImageType(ImageType.THUMBNAIL);
                    return;
                }
            }
        }

        if (!remain.isEmpty()) {
            remain.get(0).setImageType(ImageType.THUMBNAIL);
        }
    }

    public void createPostImageAtS3AndDB(List<MultipartFile> imageList, Post post) {
        List<PostImage> postImageList = new ArrayList<>();
        if (Objects.nonNull(imageList)) {
            postImageList = PostImageConverter.createPostImageList(s3Service.uploadWithThumbnail(imageList), post);
        }

        postImageRepository.saveAll(postImageList);
    }

    public List<PostImage> createPostImageNormalAtS3AndDB(List<MultipartFile> imageList, Post post) {
        List<PostImage> postImageList = new ArrayList<>();
        if (Objects.nonNull(imageList)) {
            postImageList = PostImageConverter.createAllNormal(s3Service.uploadWithThumbnail(imageList), post);
        }

        return postImageRepository.saveAll(postImageList);
    }

    public void deleteAllImageByPost(Post post) {
        List<PostImage> postImageList = postImageRepository.findByPost(post);

        List<String> urlList = collectAllUrls(postImageList);
        try {
            s3Service.delete(urlList);
        } catch (Exception e) {
            log.warn("S3 delete failed. postId={}, urls={}", post.getId(), urlList, e);
        }

        postImageRepository.deleteAllByPostId(post.getId());
    }

    public void deleteImageAtS3(List<PostImage> imageList) {
        List<String> imageUrlList = collectAllUrls(imageList);
        s3Service.delete(imageUrlList);
    }

    public void deleteImageAtDB(List<PostImage> imageList) {
        imageList.forEach(postImageRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<PostImage> findAllByPost(Post post) {
        return postImageRepository.findByPost(post);
    }

    @Transactional(readOnly = true)
    public PostImage findThumbnailImage(Post post) {
        return postImageRepository
                .findByPost_IdAndImageType(post.getId(), ImageType.THUMBNAIL)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public String findThumbnailImageUrl(Post post) {
        PostImage thumbnail = findThumbnailImage(post);
        return thumbnail != null ? resolveThumbnailUrl(thumbnail) : null;
    }

    // 목록 썸네일 우선, 없으면 원본 URL로 대체
    private static String resolveThumbnailUrl(PostImage image) {
        return image.getThumbnailUrl() != null ? image.getThumbnailUrl() : image.getImgUrl();
    }

    @Transactional(readOnly = true)
    public Map<Long, String> findThumbnailUrlByPostList(List<Post> postList) {
        if (Objects.isNull(postList) || postList.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> postIds = postList.stream().map(Post::getId).toList();

        List<PostImage> thumbnails = postImageRepository.findThumbnailImagesByPostIds(postIds);

        return thumbnails.stream()
                .collect(Collectors.toMap(
                        pi -> pi.getPost().getId(), // key: postId
                        PostImageService::resolveThumbnailUrl,
                        (a, b) -> a));
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> countImageByPostList(List<Post> postList) {
        if (Objects.isNull(postList) || postList.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> postIds = postList.stream().map(Post::getId).toList();

        return postImageRepository.countImagesGroupByPostId(postIds);
    }

    @Transactional(readOnly = true)
    public Map<Long, String> findThumbnailUrlMap(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) return Map.of();

        List<PostImage> thumbnails = postImageRepository.findByPostIdInAndImageType(postIds, ImageType.THUMBNAIL);

        return thumbnails.stream()
                .collect(Collectors.toMap(
                        pi -> pi.getPost().getId(), PostImageService::resolveThumbnailUrl, (a, b) -> a));
    }

    public void deleteImagesNotIn(Long postId, List<Long> keepImageIdList) {

        if (Objects.isNull(keepImageIdList) || keepImageIdList.isEmpty()) {
            deleteAllImages(postId);
            return;
        }

        List<PostImage> imagesToDelete = postImageRepository.findImagesToDelete(postId, keepImageIdList);

        if (imagesToDelete.isEmpty()) {
            return;
        }

        deleteFromS3(imagesToDelete);
        deleteFromDatabase(imagesToDelete);
    }

    public void deleteAllImages(Long postId) {
        List<PostImage> images = postImageRepository.findByPost_Id(postId);

        if (images.isEmpty()) return;

        deleteFromS3(images);
        postImageRepository.deleteAllByPost_Id(postId);
    }

    private void deleteFromS3(List<PostImage> images) {
        s3Service.delete(collectAllUrls(images));
    }

    // 원본 + 썸네일 URL을 모두 모아 S3 삭제 대상으로 반환 (썸네일이 없는 기존 데이터 대비 null 제외)
    private static List<String> collectAllUrls(List<PostImage> images) {
        return images.stream()
                .flatMap(pi -> Stream.of(pi.getImgUrl(), pi.getThumbnailUrl()))
                .filter(Objects::nonNull)
                .toList();
    }

    private void deleteFromDatabase(List<PostImage> images) {
        postImageRepository.deleteAll(images);
    }

    @Transactional
    public void applyThumbnailOnUpdate(Post post, Long thumbnailId, List<PostImage> newlySaved) {

        if (thumbnailId != null) {
            postImageRepository.resetThumbnailToNormal(post.getId());

            PostImage thumb = postImageRepository
                    .findByIdAndPost_Id(thumbnailId, post.getId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus._POST_IMAGE_NOT_FOUND));

            thumb.setImageType(ImageType.THUMBNAIL);
            return;
        }

        if (newlySaved != null && !newlySaved.isEmpty()) {
            Long firstId = newlySaved.get(0).getId();
            PostImage first = postImageRepository
                    .findByIdAndPost_Id(firstId, post.getId())
                    .orElseThrow(() -> new GeneralException(ErrorStatus._POST_IMAGE_NOT_FOUND));

            first.setImageType(ImageType.THUMBNAIL);
            return;
        }

        if (postImageRepository.existsThumbnailByPostId(post.getId())) {
            return;
        }

        postImageRepository
                .findFirstByPost_IdOrderByIdAsc(post.getId())
                .ifPresent(fallback -> fallback.setImageType(ImageType.THUMBNAIL));
    }
}
