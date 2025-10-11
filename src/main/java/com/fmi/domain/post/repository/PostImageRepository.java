package com.fmi.domain.post.repository;

import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.data.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage,Long> {

    List<PostImage> findByPost(Post post);
}
