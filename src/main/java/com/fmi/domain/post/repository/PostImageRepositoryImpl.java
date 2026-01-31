package com.fmi.domain.post.repository;

import com.fmi.domain.post.data.QPostImage;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PostImageRepositoryImpl implements PostImageRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Map<Long, Integer> countImagesGroupByPostId(List<Long> postIdList) {
        if (Objects.isNull(postIdList) || postIdList.isEmpty()) {
            return Collections.emptyMap();
        }

        QPostImage postImage = QPostImage.postImage;

        List<Tuple> tuples = queryFactory
                .select(postImage.post.id, postImage.id.count())
                .from(postImage)
                .where(postImage.post.id.in(postIdList))
                .groupBy(postImage.post.id)
                .fetch();

        return tuples.stream()
                .collect(Collectors.toMap(
                        t -> Objects.requireNonNull(t.get(postImage.post.id)),
                        t -> Objects.requireNonNull(t.get(postImage.id.count())).intValue()
                ));
    }
}
