package com.fmi.domain.ipblock.service;

import com.fmi.domain.ipblock.data.IpBlacklist;
import com.fmi.domain.ipblock.repository.IpBlacklistRepository;
import com.fmi.global.apiPayload.code.status.ErrorStatus;
import com.fmi.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IpBlacklistService {

    private final IpBlacklistRepository ipBlacklistRepository;

    @Transactional(readOnly = true)
    public boolean isBlocked(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        return ipBlacklistRepository.existsByIp(ip);
    }

    @Transactional
    public void blockIp(String ip, String reason) {
        if (ip == null || ip.isBlank()) {
            throw new GeneralException(ErrorStatus._INQUIRY_IP_NOT_FOUND);
        }
        if (ipBlacklistRepository.existsByIp(ip)) {
            throw new GeneralException(ErrorStatus._IP_ALREADY_BLOCKED);
        }
        ipBlacklistRepository.save(IpBlacklist.builder()
                .ip(ip)
                .reason(reason)
                .build());
    }
}
