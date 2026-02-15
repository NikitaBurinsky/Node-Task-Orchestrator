package nto.application.dto;


import nto.application.dto.base.BaseDto;

public record ServerGroupDto(
        Long id,
        String name
) implements BaseDto {}