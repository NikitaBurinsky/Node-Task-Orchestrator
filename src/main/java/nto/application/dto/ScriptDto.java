package nto.application.dto;


import nto.application.dto.base.BaseDto;

public record ScriptDto(
        Long id,
        String name,
        String content,
        Long ownerId
) implements BaseDto {}