package com.fmi.domain.chatroom.repository;

import com.fmi.domain.Enum.SortType;
import com.fmi.domain.auth.data.QUser;
import com.fmi.domain.chatmessage.data.QChatMessage;
import com.fmi.domain.chatroom.data.ChatRoomParticipant;
import com.fmi.domain.chatroom.data.QChatRoom;
import com.fmi.domain.chatroom.data.QChatRoomParticipant;
import com.fmi.domain.chatroom.data.enums.ParticipantState;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.data.QPost;
import com.querydsl.core.types.dsl.BooleanExpression;
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
    public Slice<ChatRoomParticipant> findMyChatRooms(Long userId, Long cursorId, Pageable pageable, PostType type, String address, SortType sort) {

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
                        pt.lastMessage.isNotNull(),

                        typeEq(type),
                        addressEq(address)
                );

        ChatRoomParticipant cursor = null;
        // 커서 조건
        if (cursorId != null) {
            // 커서의 시간/ID를 가져옴
            cursor = queryFactory.selectFrom(pt)
                    .where(pt.id.eq(cursorId)).fetchOne();

        }

       if(sort == SortType.OLDEST) {
           if (cursor != null) {
               // 커서 조건 추가
               query.where(
                       pt.lastMessageSentAt.gt(cursor.getLastMessageSentAt())
                               .or(pt.lastMessageSentAt.eq(cursor.getLastMessageSentAt())
                                       .and(pt.id.gt(cursor.getId())))
               );
           }
           query.orderBy(pt.lastMessageSentAt.asc(), pt.id.asc());
       }
       else {
           if (cursor != null) {
               // 커서 조건 추가
               query.where(
                       pt.lastMessageSentAt.lt(cursor.getLastMessageSentAt())
                               .or(pt.lastMessageSentAt.eq(cursor.getLastMessageSentAt())
                                       .and(pt.id.lt(cursor.getId())))
               );
           }
           query.orderBy(pt.lastMessageSentAt.desc(), pt.id.desc());
       }

        // 정렬 및 페이징 (+1 기법)
        List<ChatRoomParticipant> participants = query
                .limit(pageable.getPageSize() + 1)
                .fetch();

        //️ Slice 객체 수동 생성
        boolean hasNext = participants.size() > pageable.getPageSize();
        if (hasNext) {
            participants.remove(pageable.getPageSize());
        }

        return new SliceImpl<>(participants, pageable, hasNext);
    }

    private BooleanExpression typeEq(PostType type) {
        return type != null ? QPost.post.postType.eq(type) : null;
    }

    private BooleanExpression addressEq(String address) {
        return address != null ? QPost.post.address.startsWith(address) : null;
    }
}
