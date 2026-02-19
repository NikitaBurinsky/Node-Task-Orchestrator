package nto.application.dto;

import nto.application.dto.base.BaseDto;
import nto.core.enums.TaskStatus;

import java.time.LocalDateTime;

public record TaskDto(
    Long id,
    TaskStatus status,
    String output,
    Long serverId,
    Long scriptId,
    Long sourceGroupId,
    LocalDateTime startedAt,
    LocalDateTime finishedAt
) implements BaseDto {
}