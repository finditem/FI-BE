package com.fmi.domain.post.repository;

import java.util.List;
import java.util.Map;

public interface PostImageRepositoryCustom {
    Map<Long, Integer> countImagesGroupByPostId(List<Long> postIds);
}
