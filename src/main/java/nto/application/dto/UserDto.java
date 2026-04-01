package nto.application.dto;


import jakarta.validation.constraints.NotBlank;
import nto.application.dto.base.BaseDto;

public record UserDto(
    Long id,
    @NotBlank(message = "Username is required")
    String username,
    @NotBlank(message = "Password is required")
    String password 
) implements BaseDto {
}
