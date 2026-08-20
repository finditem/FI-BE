package com.fmi.domain.user.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.comment.data.Comment;
import com.fmi.domain.comment.repository.CommentRepository;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.repository.PostRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 30일 이상 된 삭제된 사용자를 익명화 처리 후 하드 삭제하는 스케줄러
 * 매일 자정에 실행됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletionScheduler {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    /**
     * 매일 자정(00:00)에 실행
     * 30일 이상 된 삭제된 사용자를 익명화 처리 후 하드 삭제합니다.
     */
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정
    @Transactional
    public void deleteOldDeletedUsers() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<User> usersToDelete = userRepository.findUsersDeletedBefore(thirtyDaysAgo);

        if (usersToDelete.isEmpty()) {
            log.info("하드 삭제할 사용자가 없습니다.");
            return;
        }

        int deletedCount = 0;
        for (User user : usersToDelete) {
            try {
                // 1. 게시글 익명화 처리
                anonymizeUserPosts(user);

                // 2. 댓글 익명화 처리
                anonymizeUserComments(user);

                // 3. 하드 삭제
                userRepository.delete(user);
                deletedCount++;

                log.debug("사용자 ID {} 익명화 처리 후 하드 삭제 완료", user.getId());
            } catch (Exception e) {
                log.error("사용자 ID {} 익명화 처리 중 오류 발생: {}", user.getId(), e.getMessage(), e);
                // 개별 사용자 처리 실패 시에도 다음 사용자 계속 처리
            }
        }

        log.info("30일 이상 된 삭제된 사용자 {}명을 익명화 처리 후 하드 삭제했습니다.", deletedCount);
    }

    /**
     * 사용자의 게시글 익명화 처리 (user_id를 NULL로 설정)
     */
    private void anonymizeUserPosts(User user) {
        List<Post> posts = postRepository.findByUser(user);
        if (!posts.isEmpty()) {
            for (Post post : posts) {
                post.setUser(null);
            }
            postRepository.saveAll(posts);
            log.debug("사용자 ID {}의 게시글 {}개 익명화 처리 완료", user.getId(), posts.size());
        }
    }

    /**
     * 사용자의 댓글 익명화 처리 (user_id를 NULL로 설정)
     */
    private void anonymizeUserComments(User user) {
        List<Comment> comments = commentRepository.findByUser(user);
        if (!comments.isEmpty()) {
            for (Comment comment : comments) {
                comment.setUser(null);
            }
            commentRepository.saveAll(comments);
            log.debug("사용자 ID {}의 댓글 {}개 익명화 처리 완료", user.getId(), comments.size());
        }
    }
}
