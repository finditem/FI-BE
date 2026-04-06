package com.fmi.domain.chatroom.repository;

import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.post.data.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    //Optional<ChatRoom> findByPostIdAndUserId(Long postId, Long contactUserId);

    @Query("SELECT cr FROM ChatRoom cr JOIN cr.participants p1 JOIN cr.participants p2 " +
            "WHERE cr.post.id = :postId AND p1.user.id = :userId1 AND p2.user.id = :userId2")
    Optional<ChatRoom> findChatRoomByPostAndUsers(@Param("postId") Long postId,
                                                  @Param("userId1") Long userId1,
                                                  @Param("userId2") Long userId2);


    long countByPostId(Long postId);

    List<ChatRoom> findAllByPost(Post post);
}
