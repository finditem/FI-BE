package com.fmi.domain.findcompletion.service;

import com.fmi.domain.chatroom.data.ChatRoom;
import com.fmi.domain.chatroom.repository.ChatRoomRepository;
import com.fmi.domain.findcompletion.data.FindCompletion;
import com.fmi.domain.findcompletion.exception.FindCompletionErrorStatus;
import com.fmi.domain.findcompletion.repository.FindCompletionRepository;
import com.fmi.domain.findcompletion.service.internal.FindCompletionValidator;
import com.fmi.domain.post.data.Post;
import com.fmi.domain.post.service.PostService;
import com.fmi.domain.temperature.data.TemperatureActivityType;
import com.fmi.domain.temperature.service.TemperatureService;
import com.fmi.domain.user.data.User;
import com.fmi.domain.user.repository.UserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindCompletionService {

    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final FindCompletionRepository findCompletionRepository;
    private final FindCompletionValidator findCompletionValidator;
    private final PostService postService;
    private final TemperatureService temperatureService;

    @Transactional
    public FindCompletion complete(Long chatRoomId, String userEmail) {
        User requester = userRepository
                .findByEmail(userEmail)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        ChatRoom chatRoom = chatRoomRepository
                .findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._CHATROOM_NOT_FOUND));

        findCompletionValidator.validate(chatRoom, requester);

        Post post = chatRoom.getPost();
        FindCompletion findCompletion = FindCompletion.builder()
                .sourcePostId(post.getId())
                .chatRoom(chatRoom)
                .requester(requester)
                .helper(chatRoom.getOtherParticipant(requester.getId()))
                .build();
        save(findCompletion);
        temperatureService.record(
                TemperatureActivityType.RETURN_COMPLETED,
                findCompletion.getHelper().getId(),
                findCompletion.getId(),
                findCompletion.getCreatedAt());

        postService.markAsFound(post);
        return findCompletion;
    }

    private void save(FindCompletion findCompletion) {
        try {
            findCompletionRepository.saveAndFlush(findCompletion);
        } catch (DataIntegrityViolationException e) {
            // 다른 채팅방에서 같은 게시글을 동시에 완료하면 uk_find_completion_source_post에서 막힌다.
            throw new GeneralException(FindCompletionErrorStatus.ALREADY_COMPLETED);
        }
    }
}
