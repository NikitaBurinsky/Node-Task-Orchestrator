package nto.application.dto;


import nto.application.dto.base.BaseDto;

public record UserDto(
    Long id,
    String username,
    String password // В реальном проекте password исключают или используют @JsonIgnore на чтение
) implements BaseDto {
}