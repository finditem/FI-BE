package com.fmi.domain.post.repository;

import com.fmi.domain.post.data.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage,Long> {

}
