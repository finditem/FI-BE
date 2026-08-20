package com.fmi.domain.post.converter;

import com.fmi.domain.post.data.Radius;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Objects;

@Converter(autoApply = true)
public class RadiusConverter implements AttributeConverter<Radius, Integer> {
    @Override
    public Integer convertToDatabaseColumn(Radius attribute) {
        return Objects.isNull(attribute) ? null : attribute.getValue();
    }

    @Override
    public Radius convertToEntityAttribute(Integer dbData) {
        return Objects.isNull(dbData) ? null : Radius.from(dbData);
    }
}
