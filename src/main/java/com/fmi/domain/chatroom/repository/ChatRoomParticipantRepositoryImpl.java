package com.fmi.domain.chatroom.repository;

import com.fmi.domain.Enum.SortType;
import com.fmi.domain.chatmessage.data.QChatMessage;
import com.fmi.domain.chatroom.data.ChatRoomParticipant;
import com.fmi.domain.chatroom.data.QChatRoom;
import com.fmi.domain.chatroom.data.QChatRoomParticipant;
import com.fmi.domain.chatroom.data.enums.ParticipantState;
import com.fmi.domain.post.data.PostType;
import com.fmi.domain.post.data.QPost;
import com.fmi.domain.user.data.QUser;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
public class ChatRoomParticipantRepositoryImpl implements ChatRoomParticipantRepositoryCustom {

    private static final Map<String, String> ADDRESS_ALIASES = Map.ofEntries(
            Map.entry("서울시", "서울특별시"),
            Map.entry("부산시", "부산광역시"),
            Map.entry("대구시", "대구광역시"),
            Map.entry("인천시", "인천광역시"),
            Map.entry("광주시", "광주광역시"),
            Map.entry("대전시", "대전광역시"),
            Map.entry("울산시", "울산광역시"),
            Map.entry("세종시", "세종특별자치시"),
            Map.entry("강원도", "강원특별자치도"),
            Map.entry("전라북도", "전북특별자치도"),
            Map.entry("제주도", "제주특별자치도"));

    private final JPAQueryFactory queryFactory;

    public ChatRoomParticipantRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Slice<ChatRoomParticipant> findMyChatRooms(
            Long userId, Long cursorId, Pageable pageable, PostType type, String address, SortType sort) {

        QChatRoomParticipant pt = QChatRoomParticipant.chatRoomParticipant;
        QChatRoom cr = QChatRoom.chatRoom;
        QPost p = QPost.post;
        QChatMessage lm = QChatMessage.chatMessage;
        QChatRoomParticipant otherPt = new QChatRoomParticipant("otherPt");
        QUser otherUser = QUser.user;

        JPAQuery<ChatRoomParticipant> query = queryFactory
                .selectFrom(pt)
                .join(pt.chatRoom, cr)
                .fetchJoin()
                .leftJoin(cr.post, p)
                .fetchJoin()
                .join(pt.lastMessage, lm)
                .fetchJoin()
                .join(cr.participants, otherPt)
                .join(otherPt.user, otherUser)
                .fetchJoin()
                .where(
                        // 공통 WHERE 조건
                        pt.user.id.eq(userId),
                        otherPt.user.id.ne(userId),
                        pt.participantState.eq(ParticipantState.ACTIVE),
                        pt.lastMessage.isNotNull(),
                        typeEq(type, p, cr),
                        addressEq(address, p, cr));

        ChatRoomParticipant cursor = null;
        // 커서 조건
        if (cursorId != null) {
            // 커서의 시간/ID를 가져옴
            cursor = queryFactory.selectFrom(pt).where(pt.id.eq(cursorId)).fetchOne();
        }

        if (sort == SortType.OLDEST) {
            if (cursor != null) {
                // 커서 조건 추가
                query.where(pt.lastMessageSentAt
                        .gt(cursor.getLastMessageSentAt())
                        .or(pt.lastMessageSentAt
                                .eq(cursor.getLastMessageSentAt())
                                .and(pt.id.gt(cursor.getId()))));
            }
            query.orderBy(pt.lastMessageSentAt.asc(), pt.id.asc());
        } else {
            if (cursor != null) {
                // 커서 조건 추가
                query.where(pt.lastMessageSentAt
                        .lt(cursor.getLastMessageSentAt())
                        .or(pt.lastMessageSentAt
                                .eq(cursor.getLastMessageSentAt())
                                .and(pt.id.lt(cursor.getId()))));
            }
            query.orderBy(pt.lastMessageSentAt.desc(), pt.id.desc());
        }

        // 정렬 및 페이징 (+1 기법)
        List<ChatRoomParticipant> participants =
                query.limit(pageable.getPageSize() + 1).fetch();

        // ️ Slice 객체 수동 생성
        boolean hasNext = participants.size() > pageable.getPageSize();
        if (hasNext) {
            participants.remove(pageable.getPageSize());
        }

        return new SliceImpl<>(participants, pageable, hasNext);
    }

    private BooleanExpression typeEq(PostType type, QPost p, QChatRoom cr) {
        if (type == null) return null;
        return cr.sourcePostDeleted
                .isFalse()
                .and(p.postType.eq(type))
                .or(cr.sourcePostDeleted.isTrue().or(p.id.isNull()).and(cr.sourcePostType.eq(type)));
    }

    private BooleanExpression addressEq(String address, QPost p, QChatRoom cr) {
        if (address == null || address.isBlank()) return null;

        String[] tokens = address.trim().split("\\s+");
        BooleanExpression result = null;

        for (String token : tokens) {
            String normalized = ADDRESS_ALIASES.getOrDefault(token, token);
            BooleanExpression oneToken = cr.sourcePostDeleted
                    .isFalse()
                    .and(p.address.contains(normalized))
                    .or(cr.sourcePostDeleted.isTrue().or(p.id.isNull()).and(cr.sourceAddress.contains(normalized)));
            result = (result == null) ? oneToken : result.and(oneToken);
        }

        return result;
    }
}
