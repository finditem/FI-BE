package com.fmi.domain.review.repository;

import static com.fmi.domain.review.data.QReview.review;

import com.fmi.domain.review.data.QReview;
import com.fmi.domain.review.data.ReviewHelpType;
import com.fmi.domain.review.data.ReviewSummary;
import com.fmi.domain.user.data.QUser;
import com.fmi.global.data.EntityStatus;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReviewRepositoryImpl implements ReviewRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ReviewSummary> findReceivedReviews(Long recipientId, boolean excludeHidden, Long cursor, int limit) {
        // 받은 후기에는 작성자를 상대방으로 보여준다. 하드 삭제된 작성자도 포함하도록 left join한다.
        QUser author = new QUser("author");
        return queryFactory
                .select(summary(author))
                .from(review)
                .leftJoin(review.author, author)
                .where(receivedBy(recipientId, excludeHidden), beforeCursor(cursor))
                .orderBy(review.createdAt.desc(), review.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public long countReceivedReviews(Long recipientId, boolean excludeHidden) {
        Long count = queryFactory
                .select(review.id.count())
                .from(review)
                .where(receivedBy(recipientId, excludeHidden))
                .fetchOne();
        return Objects.requireNonNullElse(count, 0L);
    }

    @Override
    public List<ReviewSummary> findSentReviews(Long authorId, Long cursor, int limit) {
        // 보낸 후기에는 받은 사용자를 상대방으로 보여준다.
        QUser recipient = new QUser("recipient");
        return queryFactory
                .select(summary(recipient))
                .from(review)
                .leftJoin(review.recipient, recipient)
                .where(sentBy(authorId), beforeCursor(cursor))
                .orderBy(review.createdAt.desc(), review.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public long countSentReviews(Long authorId) {
        Long count = queryFactory
                .select(review.id.count())
                .from(review)
                .where(sentBy(authorId))
                .fetchOne();
        return Objects.requireNonNullElse(count, 0L);
    }

    @Override
    public Map<Long, Set<ReviewHelpType>> findHelpTypesByReviewIds(List<Long> reviewIds) {
        if (reviewIds.isEmpty()) {
            return Map.of();
        }

        EnumPath<ReviewHelpType> helpType = Expressions.enumPath(ReviewHelpType.class, "helpType");
        List<Tuple> rows = queryFactory
                .select(review.id, helpType)
                .from(review)
                .join(review.helpTypes, helpType)
                .where(review.id.in(reviewIds))
                .fetch();

        Map<Long, Set<ReviewHelpType>> helpTypesByReviewId = new HashMap<>();
        for (Tuple row : rows) {
            helpTypesByReviewId
                    .computeIfAbsent(row.get(review.id), id -> EnumSet.noneOf(ReviewHelpType.class))
                    .add(row.get(helpType));
        }
        return helpTypesByReviewId;
    }

    private ConstructorExpression<ReviewSummary> summary(QUser user) {
        return Projections.constructor(
                ReviewSummary.class,
                review.id,
                review.emotion,
                review.content,
                review.createdAt,
                user.id,
                user.nickname,
                user.profile_img,
                user.deletedAt);
    }

    private BooleanExpression receivedBy(Long recipientId, boolean excludeHidden) {
        BooleanExpression condition =
                review.recipient.id.eq(recipientId).and(review.entityStatus.eq(EntityStatus.ACTIVE));
        return excludeHidden ? condition.and(review.hiddenByRecipient.isFalse()) : condition;
    }

    private BooleanExpression sentBy(Long authorId) {
        return review.author
                .id
                .eq(authorId)
                .and(review.entityStatus.eq(EntityStatus.ACTIVE))
                .and(review.hiddenByAuthor.isFalse());
    }

    // 커서 후기보다 뒤에 오는 후기: 작성일시가 더 이르거나, 같으면 id가 더 작다.
    private BooleanExpression beforeCursor(Long cursor) {
        if (cursor == null) {
            return null;
        }

        QReview cursorReview = new QReview("cursorReview");
        JPQLQuery<LocalDateTime> cursorCreatedAt =
                JPAExpressions.select(cursorReview.createdAt).from(cursorReview).where(cursorReview.id.eq(cursor));
        return review.createdAt
                .lt(cursorCreatedAt)
                .or(review.createdAt.eq(cursorCreatedAt).and(review.id.lt(cursor)));
    }
}
