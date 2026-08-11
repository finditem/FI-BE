package com.fmi.domain.chatroom.repository;

import com.fmi.domain.chatroom.data.QChatRoom;
import com.fmi.domain.post.data.QPost;
import com.querydsl.core.types.dsl.BooleanExpression;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ChatRoomParticipantRepositoryImplTest {

    @Mock
    private EntityManager entityManager;

    private ChatRoomParticipantRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ChatRoomParticipantRepositoryImpl(entityManager);
    }

    private BooleanExpression invokeAddressEq(String address) throws Exception {
        Method method = ChatRoomParticipantRepositoryImpl.class
                .getDeclaredMethod("addressEq", String.class, QPost.class, QChatRoom.class);
        method.setAccessible(true);
        return (BooleanExpression) method.invoke(repository, address, QPost.post, QChatRoom.chatRoom);
    }

    @Test
    @DisplayName("address가 null이면 null 반환")
    void addressEq_null() throws Exception {
        assertThat(invokeAddressEq(null)).isNull();
    }

    @Test
    @DisplayName("address가 빈 문자열이면 null 반환")
    void addressEq_blank() throws Exception {
        assertThat(invokeAddressEq("   ")).isNull();
    }

    @Test
    @DisplayName("단일 키워드 검색 시 해당 키워드를 포함하는 조건 생성")
    void addressEq_singleToken() throws Exception {
        BooleanExpression result = invokeAddressEq("강남");

        assertThat(result).isNotNull();
        assertThat(result.toString()).contains("강남");
    }

    @Test
    @DisplayName("서울시 → 서울특별시로 정규화")
    void addressEq_aliasNormalization_seoul() throws Exception {
        BooleanExpression result = invokeAddressEq("서울시");

        assertThat(result).isNotNull();
        assertThat(result.toString()).contains("서울특별시");
        assertThat(result.toString()).doesNotContain("서울시");
    }

    @Test
    @DisplayName("별칭이 없는 키워드는 그대로 사용")
    void addressEq_noAlias() throws Exception {
        BooleanExpression result = invokeAddressEq("개포동");

        assertThat(result).isNotNull();
        assertThat(result.toString()).contains("개포동");
    }

    @Test
    @DisplayName("다중 키워드 검색 시 모든 키워드를 포함하는 AND 조건 생성")
    void addressEq_multipleTokens() throws Exception {
        BooleanExpression result = invokeAddressEq("강남 개포");

        assertThat(result).isNotNull();
        assertThat(result.toString()).contains("강남");
        assertThat(result.toString()).contains("개포");
    }

    @Test
    @DisplayName("별칭 키워드와 일반 키워드 혼합 시 정규화 + AND 조건 생성")
    void addressEq_aliasWithMultipleTokens() throws Exception {
        BooleanExpression result = invokeAddressEq("서울시 강남");

        assertThat(result).isNotNull();
        assertThat(result.toString()).contains("서울특별시");
        assertThat(result.toString()).contains("강남");
        assertThat(result.toString()).doesNotContain("서울시 강남");
    }
}
