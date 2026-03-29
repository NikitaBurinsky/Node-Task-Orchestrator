package nto.application.dto;
import jakarta.validation.constraints.NotEmpty;
import nto.application.dto.base.BaseDto;

public record ScriptDto(
    Long id,
    @NotEmpty
    String name,
    @NotEmpty
    String content,
    String ownerName,
    Boolean isPublic
) implements BaseDto {
}
