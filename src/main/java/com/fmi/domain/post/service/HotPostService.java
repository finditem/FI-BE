package com.fmi.domain.post.service;

import com.fmi.domain.Enum.Category;
import com.fmi.domain.post.data.PostStatus;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.repository.PostHotQueryRepository;
import com.fmi.domain.userblock.repository.BlockedUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class HotPostService {
    private final PostHotQueryRepository postHotQueryRepository;
    private final BlockedUserRepository blockedUserRepository;

    @Cacheable(
            cacheNames = "hotPostIds",
            key = "T(java.util.Objects).toString(#postType)+'|'+#postStatus+'|'+#category+'|'+#address"
    )
    public List<Long> getHotPostIdCandidates(PostType postType, PostStatus postStatus,
                                             Category category, String address, int candidateSize) {
        return postHotQueryRepository.findHotPostIds(postType, postStatus, category, address, candidateSize);
    }

    public Set<Long> resolveHotPostIdsForUser(PostType postType, PostStatus postStatus,
                                              Category category, String address,
                                              Long userId, int hotSize) {

        List<Long> candidates = getHotPostIdCandidates(postType, postStatus, category, address, 50);

        if (candidates.isEmpty()) return Set.of();

        if (userId == null) {
            return new LinkedHashSet<>(candidates.subList(0, Math.min(hotSize, candidates.size())));
        }

        Set<Long> excludedUserIds = new HashSet<>();
        excludedUserIds.addAll(blockedUserRepository.findBlockedUserIdsByBlockerId(userId));
        excludedUserIds.addAll(blockedUserRepository.findBlockerIdsByBlockedId(userId));

        Map<Long, Long> authorByPostId = postHotQueryRepository.findAuthorIdsByPostIds(candidates);

        LinkedHashSet<Long> result = new LinkedHashSet<>();
        for (Long postId : candidates) {
            Long authorId = authorByPostId.get(postId);
            if (authorId == null || excludedUserIds.contains(authorId)) continue;

            result.add(postId);
            if (result.size() == hotSize) break;
        }
        return result;
    }
}
