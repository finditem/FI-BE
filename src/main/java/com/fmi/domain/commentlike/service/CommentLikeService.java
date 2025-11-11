package com.fmi.domain.commentlike.service;


import com.fmi.domain.notification.service.NotificationService;
import com.fmi.domain.commentlike.repository.CommentLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CommentLikeService {

    private final CommentLikeRepository postLikeRepository;
    private final RedisTemplate redisTemplate;
    private final NotificationService notificationService;


}