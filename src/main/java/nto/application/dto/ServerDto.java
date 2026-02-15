package nto.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import nto.application.dto.base.BaseDto;

public record ServerDto (
        Long id,

        @NotBlank(message = "Hostname cannot be empty")
        String hostname,

        @NotBlank(message = "IP address is required")
        // Регулярка для IPv4
        @Pattern(regexp = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$",
                message = "Invalid IPv4 format")
        String ipAddress,

        @NotNull
        @Min(1) @Max(65535)
        Integer port
) implements BaseDto {
}