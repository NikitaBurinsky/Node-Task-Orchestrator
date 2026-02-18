package nto.infrastructure.mapping.profiles;

import nto.application.dto.ServerGroupDto;
import nto.application.interfaces.mapping.MapperProfile;
import nto.core.entities.ServerEntity;
import nto.core.entities.ServerGroupEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ServerGroupProfile implements MapperProfile<ServerGroupEntity, ServerGroupDto> {

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
                entity.getServers().stream()
                        .map(ServerEntity::getId)
                        .collect(Collectors.toList())
        );
    }

    @Override
    public ServerGroupEntity mapToEntity(ServerGroupDto dto) {
        // Owner и Servers устанавливаются в сервисе
        return ServerGroupEntity.builder()
                .name(dto.name())
                .build();
    }

    @Override
    public void mapToEntity(ServerGroupDto dto, ServerGroupEntity entity) {
        entity.setName(dto.name());
        // Состав группы обновляется через отдельные методы сервиса addServer/removeServer
    }
}