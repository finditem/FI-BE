package com.fmi.domain.userblock.service;

import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.userblock.data.BlockedUser;
import com.fmi.domain.userblock.repository.BlockedUserRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockService {

    private final BlockedUserRepository blockedUserRepository;
    private final UserRepository userRepository;

    @Transactional
    public void block(Long blockerUserId, Long targetUserId) {
        if (blockerUserId.equals(targetUserId)) {
            throw new GeneralException(ErrorStatus._USER_BLOCK_SELF);
        }
        User blocker = userRepository.findById(blockerUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        // 단방향이든 이미 있으면 넘어가되, 항상 양방향 상태가 되도록 맞춘다
        if (!blockedUserRepository.existsByBlockerAndBlocked(blocker, target)) {
            BlockedUser bu = BlockedUser.builder()
                    .blocker(blocker)
                    .blocked(target)
                    .build();
            blockedUserRepository.save(bu);
        }
        if (!blockedUserRepository.existsByBlockerAndBlocked(target, blocker)) {
            BlockedUser bu2 = BlockedUser.builder()
                    .blocker(target)
                    .blocked(blocker)
                    .build();
            blockedUserRepository.save(bu2);
        }
    }

    @Transactional
    public void unblock(Long blockerUserId, Long targetUserId) {
        User blocker = userRepository.findById(blockerUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        // 양방향 모두 해제 (없으면 무시: idempotent)
        blockedUserRepository.findByBlockerAndBlocked(blocker, target)
                .ifPresent(blockedUserRepository::delete);
        blockedUserRepository.findByBlockerAndBlocked(target, blocker)
                .ifPresent(blockedUserRepository::delete);
    }

    public List<BlockedUser> list(Long blockerUserId) {
        User blocker = userRepository.findById(blockerUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        return blockedUserRepository.findAllByBlocker(blocker);
    }

    public boolean isBlocked(Long blockerUserId, Long otherUserId) {
        User blocker = userRepository.findById(blockerUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        User other = userRepository.findById(otherUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        // 양방향 중 하나라도 존재하면 차단된 것으로 간주
        return blockedUserRepository.existsByBlockerAndBlocked(blocker, other)
                || blockedUserRepository.existsByBlockerAndBlocked(other, blocker);
    }
}


