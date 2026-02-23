package nto.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import nto.application.dto.base.BaseDto;

public record UpdateServerDto(
    String hostname,

    String ipAddress,

    @Min(1) @Max(65535)
    Integer port,

    @NotBlank String username,

    String password
) implements BaseDto {
}