package com.fmi.domain.postlike.service;


import com.fmi.domain.notification.service.NotificationService;
import com.fmi.domain.postlike.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final RedisTemplate redisTemplate;
    private final NotificationService notificationService;

    public void addLike(Long postId, Long userId) {

        postLikeRepository.existsByPostIdAndUserId(postId, userId);

        // Redis에 임시 저장
        String key = "like:post:" + postId;
        redisTemplate.opsForSet().add(key, userId);
        redisTemplate.expire(key, 5, TimeUnit.MINUTES);
    }

    @Scheduled(fixedRate = 60000)  // 1분마다 실행
    public void processLikeNotifications() {
        Set<String> keys = redisTemplate.keys("like:post:*");
        if (keys == null) return;

        for (String key : keys) {
            Set<Object> userIds = redisTemplate.opsForSet().members(key);
            if (userIds == null || userIds.isEmpty()) continue;

//            Long postId = Long.parseLong(key.split(":")[2]);
//            String message = notificationConverter.convertLikeNotification(postId, userIds);
//
//            notificationRepository.save(Notification.builder()
//                    .postId(postId)
//                    .message(message)
//                    .build());
//
//            pushService.sendNotification(postId, message); // 푸시 전송

            redisTemplate.delete(key); // 처리 후 삭제
        }
    }
}