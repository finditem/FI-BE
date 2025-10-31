package com.fmi.domain.chatroom.repository;

import com.fmi.domain.auth.data.QUser;
import com.fmi.domain.chatmessage.data.QChatMessage;
import com.fmi.domain.chatroom.data.ChatRoomParticipant;
import com.fmi.domain.chatroom.data.QChatRoom;
import com.fmi.domain.chatroom.data.QChatRoomParticipant;
import com.fmi.domain.chatroom.data.enums.ParticipantState;
import com.fmi.domain.post.data.QPost;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ChatRoomParticipantRepositoryImpl implements ChatRoomParticipantRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public ChatRoomParticipantRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Slice<ChatRoomParticipant> findMyChatRooms(Long userId, Long cursorId, Pageable pageable) {

        QChatRoomParticipant pt = QChatRoomParticipant.chatRoomParticipant;
        QChatRoom cr = QChatRoom.chatRoom;
        QPost p = QPost.post;
        QChatMessage lm = QChatMessage.chatMessage;
        QChatRoomParticipant otherPt = new QChatRoomParticipant("otherPt");
        QUser otherUser = QUser.user;

        JPAQuery<ChatRoomParticipant> query = queryFactory
                .selectFrom(pt)
                .join(pt.chatRoom, cr).fetchJoin()
                .join(cr.post, p).fetchJoin()
                .join(pt.lastMessage, lm).fetchJoin()
                .join(cr.participants, otherPt)
                .join(otherPt.user, otherUser).fetchJoin()
                .where(
                        // 공통 WHERE 조건
                        pt.user.id.eq(userId),
                        otherPt.user.id.ne(userId),
                        pt.participantState.eq(ParticipantState.ACTIVE),
                        pt.lastMessage.isNotNull()
                );

        // 커서 조건
        if (cursorId != null) {
            // 커서의 시간/ID를 가져옴
            ChatRoomParticipant cursor = queryFactory.selectFrom(pt)
                    .where(pt.id.eq(cursorId)).fetchOne();

            if (cursor != null) {
                // 커서 조건 추가
                query.where(
                        pt.lastMessageSentAt.lt(cursor.getLastMessageSentAt())
                                .or(pt.lastMessageSentAt.eq(cursor.getLastMessageSentAt())
                                        .and(pt.id.lt(cursor.getId())))
                );
            }
        }

        // 정렬 및 페이징 (+1 기법)
        List<ChatRoomParticipant> participants = query
                .orderBy(pt.lastMessageSentAt.desc(), pt.id.desc())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        //️ Slice 객체 수동 생성
        boolean hasNext = participants.size() > pageable.getPageSize();
        if (hasNext) {
            participants.remove(pageable.getPageSize());
        }

        return new SliceImpl<>(participants, pageable, hasNext);
    }
}
