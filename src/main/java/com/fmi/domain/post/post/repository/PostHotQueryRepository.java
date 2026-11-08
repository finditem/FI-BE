package com.fmi.domain.post.post.repository;

import com.fmi.domain.post.post.data.Post;
import org.springframework.data.repository.Repository;

public interface PostHotQueryRepository extends Repository<Post, Long>, PostHotQueryRepositoryCustom {}
