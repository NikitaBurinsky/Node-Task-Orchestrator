package nto.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequestDto(
    @NotBlank(message = "Username is required")
    String username,
    @NotBlank(message = "Password is required")
    String password
) {
}
