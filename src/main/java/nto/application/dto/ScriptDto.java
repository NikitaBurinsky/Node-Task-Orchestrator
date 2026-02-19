package nto.application.dto;

import nto.application.dto.base.BaseDto;

public record ScriptDto(
    Long id,
    String name,
    String content,
    String ownerName, // Вместо ownerId
    Boolean isPublic
) implements BaseDto {
}