package nto.infrastructure.mapping.profiles;

import lombok.RequiredArgsConstructor;
import nto.application.dto.ServerGroupDto;
import nto.application.interfaces.mapping.MapperProfile;
import nto.core.entities.ServerGroupEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor // Добавляем lombok конструктор для инъекции
public class ServerGroupProfile implements MapperProfile<ServerGroupEntity, ServerGroupDto> {

    private final ServerProfile serverProfile; // Инжектим маппер серверов

    @Override
    public Class<ServerGroupEntity> getEntityClass() {
        return ServerGroupEntity.class;
    }

    @Override
    public Class<ServerGroupDto> getDtoClass() {
        return ServerGroupDto.class;
    }

    @Override
    public ServerGroupDto mapToDto(ServerGroupEntity entity) {
        return new ServerGroupDto(
                entity.getId(),
                entity.getName(),
                // Преобразуем список Entity серверов в список DTO
                entity.getServers() != null
                        ? entity.getServers().stream()
                        .map(serverProfile::mapToDto)
                        .collect(Collectors.toList())
                        : Collections.emptyList()
        );
    }

    @Override
    public ServerGroupEntity mapToEntity(ServerGroupDto dto) {
        return ServerGroupEntity.builder()
                .name(dto.name())
                .build();
    }

    @Override
    public void mapToEntity(ServerGroupDto dto, ServerGroupEntity entity) {
        entity.setName(dto.name());
    }
}