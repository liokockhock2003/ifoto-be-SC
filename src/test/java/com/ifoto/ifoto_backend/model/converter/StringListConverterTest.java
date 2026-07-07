package com.ifoto.ifoto_backend.model.converter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StringListConverterTest {

    private final StringListConverter converter = new StringListConverter();

    @Test
    void convertToDatabaseColumn_thenConvertToEntityAttribute_roundTripsSameList() {
        List<String> original = List.of("Canon EOS R5", "Sony A7 IV", "Nikon Z6");

        String json = converter.convertToDatabaseColumn(original);
        List<String> restored = converter.convertToEntityAttribute(json);

        assertThat(restored).containsExactlyElementsOf(original);
    }

    @Test
    void convertToDatabaseColumn_withNullList_returnsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void convertToDatabaseColumn_withEmptyList_returnsNull() {
        assertThat(converter.convertToDatabaseColumn(List.of())).isNull();
    }

    @Test
    void convertToEntityAttribute_withNullOrBlankInput_returnsEmptyList() {
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
        assertThat(converter.convertToEntityAttribute("   ")).isEmpty();
    }

    @Test
    void convertToEntityAttribute_withInvalidJson_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("not valid json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Could not deserialize JSON to list");
    }
}