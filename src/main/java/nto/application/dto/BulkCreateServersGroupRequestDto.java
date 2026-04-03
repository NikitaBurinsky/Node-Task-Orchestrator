package nto.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkCreateServersGroupRequestDto(
    @NotBlank(message = "Name is required")
    String name,

    @NotEmpty(message = "Server list cannot be empty")
    List<@Valid ServerDto> servers
) {
}
