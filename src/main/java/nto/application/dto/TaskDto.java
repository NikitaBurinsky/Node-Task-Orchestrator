package nto.application.dto;

import nto.core.enums.TaskStatus;

public record TaskDto(
        Long id,
        TaskStatus status,
        String output,
        Long serverId,
        Long scriptId
) {}