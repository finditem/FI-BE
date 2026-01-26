package com.fmi.global.converter;

import com.fmi.domain.Enum.UserOtherPageType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class UserOtherPageTypeConverter implements Converter<String, UserOtherPageType> {
    @Override
    public UserOtherPageType convert(@NonNull String source) {
        return UserOtherPageType.fromParam(source);
    }
}
