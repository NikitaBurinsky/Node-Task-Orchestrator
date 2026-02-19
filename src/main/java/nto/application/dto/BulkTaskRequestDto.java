package nto.application.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BulkTaskRequestDto(
    @NotNull(message = "Script ID is required")
    Long scriptId,

    @NotEmpty(message = "Server list cannot be empty")
    List<Long> serverIds
) {
}