package com.fmi.domain.chat.message.service.result;

import com.fmi.domain.user.user.data.enums.LanguageCode;

public record ChatTranslationSourceResult(Long userId, String text, LanguageCode language) {}
