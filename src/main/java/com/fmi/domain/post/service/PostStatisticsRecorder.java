package com.fmi.domain.post.service;

import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.repository.PostStatisticsRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostStatisticsRecorder {
    private final PostStatisticsRepository repository;

    public void recordPublication(PostType type, LocalDateTime occurredAt) {
        long lost = 0;
        long found = 0;
        if (type == PostType.LOST) lost = 1;
        else found = 1;
        repository.increment(occurredAt.toLocalDate(), lost, found, 0);
    }

    public void recordRemoval(long count, LocalDateTime occurredAt) {
        if (count == 0) return;
        repository.increment(occurredAt.toLocalDate(), 0, 0, count);
    }
}
