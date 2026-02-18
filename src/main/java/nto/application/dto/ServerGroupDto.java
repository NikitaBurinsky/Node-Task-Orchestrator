package nto.application.dto;

import jakarta.validation.constraints.NotBlank;
import nto.application.dto.base.BaseDto;
import java.util.List;

public record ServerGroupDto(
        Long id,
        @NotBlank(message = "Name is required")
        String name,
        List<Long> serverIds // ID серверов, входящих в группу
) implements BaseDto {}