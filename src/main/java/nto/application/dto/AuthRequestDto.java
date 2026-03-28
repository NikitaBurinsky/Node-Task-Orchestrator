package nto.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequestDto(
    String username,
    @NotBlank
    String password
) {
}
