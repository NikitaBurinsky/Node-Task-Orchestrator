package nto.application.dto;


import nto.application.dto.base.BaseDto;

public record UserDto(
    Long id,
    String username,
    String password 
) implements BaseDto {
}