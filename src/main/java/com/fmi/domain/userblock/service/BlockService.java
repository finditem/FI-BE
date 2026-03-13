package com.fmi.domain.userblock.service;

import com.fmi.domain.Enum.Role;
import com.fmi.domain.auth.data.User;
import com.fmi.domain.auth.repository.UserRepository;
import com.fmi.domain.userblock.data.BlockedUser;
import com.fmi.domain.userblock.repository.BlockedUserRepository;
import com.fmi.domain.userblock.web.dto.response.BlockedUserResponse;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fmi.global.apiPayload.CursorPageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.util.List;

@Slf4j
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
        User blocker = userRepository.findActiveById(blockerUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        User target = userRepository.findActiveById(targetUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        if (target.getRole() == Role.ADMIN) {
            throw new GeneralException(ErrorStatus._CANNOT_BLOCK_ADMIN);
        }

        saveBlockIfNotExists(blocker, target);
        saveBlockIfNotExists(target, blocker);
    }

    @Transactional
    public void unblock(Long blockerUserId, Long targetUserId) {
        User blocker = userRepository.findActiveById(blockerUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        User target = userRepository.findActiveById(targetUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        // 양방향 모두 해제 (없으면 무시: idempotent)
        blockedUserRepository.findByBlockerAndBlocked(blocker, target)
                .ifPresent(blockedUserRepository::delete);
        blockedUserRepository.findByBlockerAndBlocked(target, blocker)
                .ifPresent(blockedUserRepository::delete);
    }

    public List<BlockedUser> list(Long blockerUserId) {
        User blocker = userRepository.findActiveById(blockerUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        return blockedUserRepository.findAllByBlocker(blocker);
    }

    public List<BlockedUserResponse> listWithUserInfo(Long blockerUserId) {
        User blocker = userRepository.findActiveById(blockerUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        return blockedUserRepository.findAllByBlocker(blocker).stream()
                .map(bu -> new BlockedUserResponse(
                        bu.getBlocked().getId(),
                        bu.getBlocked().getNickname(),
                        bu.getBlocked().getProfile_img()
                ))
                .toList();
    }

    public CursorPageResponse<BlockedUserResponse> listWithUserInfoCursor(Long blockerUserId, Long cursor, int size) {
        User blocker = userRepository.findActiveById(blockerUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));

        PageRequest pageRequest = PageRequest.of(0, size);
        Slice<BlockedUser> slice = (cursor == null)
                ? blockedUserRepository.findByBlockerOrderByIdDesc(blocker, pageRequest)
                : blockedUserRepository.findByBlockerAndIdLessThanOrderByIdDesc(blocker, cursor, pageRequest);

        List<BlockedUserResponse> content = slice.getContent().stream()
                .map(bu -> new BlockedUserResponse(
                        bu.getBlocked().getId(),
                        bu.getBlocked().getNickname(),
                        bu.getBlocked().getProfile_img()
                ))
                .toList();

        Long nextCursor = slice.hasNext() ? slice.getContent().get(slice.getContent().size() - 1).getId() : null;
        return new CursorPageResponse<>(content, nextCursor, slice.hasNext());
    }

    private void saveBlockIfNotExists(User blocker, User blocked) {
        if (blockedUserRepository.existsByBlockerAndBlocked(blocker, blocked)) {
            return;
        }
        try {
            blockedUserRepository.save(BlockedUser.builder()
                    .blocker(blocker)
                    .blocked(blocked)
                    .build());
        } catch (DataIntegrityViolationException e) {
            log.debug("차단 레코드 이미 존재: blocker={}, blocked={}", blocker.getId(), blocked.getId());
        }
    }

    public boolean isBlocked(Long blockerUserId, Long otherUserId) {
        User blocker = userRepository.findActiveById(blockerUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        User other = userRepository.findActiveById(otherUserId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        // 양방향 중 하나라도 존재하면 차단된 것으로 간주
        return blockedUserRepository.existsByBlockerAndBlocked(blocker, other)
                || blockedUserRepository.existsByBlockerAndBlocked(other, blocker);
    }
}


