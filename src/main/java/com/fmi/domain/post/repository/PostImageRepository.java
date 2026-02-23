package com.fmi.domain.post.repository;

import com.fmi.domain.post.data.ImageType;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostImage;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long>, PostImageRepositoryCustom {

    List<PostImage> findByPost(Post post);

    Optional<PostImage> findByPost_IdAndImageType(Long postId, ImageType imageType);

    @Query("""
            select pi
            from PostImage pi
            where pi.post.id in :postIds
              and pi.imageType = com.fmi.domain.post.data.ImageType.THUMBNAIL
            """)
    List<PostImage> findThumbnailImagesByPostIds(@Param("postIds") List<Long> postIds);

    @Modifying
    @Query("delete from PostImage pi where pi.post = :post")
    void deleteAllByPost(@Param("post") Post post);

    List<PostImage> findByPostIdInAndImageType(List<Long> postIds, ImageType imageType);

    @Query("""
                select pi
                from PostImage pi
                where pi.post.id = :postId
                and pi.id not in :keepIds
            """)
    List<PostImage> findImagesToDelete(@Param("postId") Long postId,
                                       @Param("keepIds") List<Long> keepIds);

    void deleteAllByPost_Id(Long postId);

    List<PostImage> findByPost_Id(Long postId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PostImage pi set pi.imageType = 'NORMAL' where pi.post.id = :postId and pi.imageType = 'THUMBNAIL'")
    void resetThumbnailToNormal(@Param("postId") Long postId);

    Optional<PostImage> findByIdAndPost_Id(Long id, Long postId);

}
