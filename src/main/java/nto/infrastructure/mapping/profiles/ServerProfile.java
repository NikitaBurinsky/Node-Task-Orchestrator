package nto.infrastructure.mapping.profiles;

import nto.application.dto.ServerDto;
import nto.application.interfaces.mapping.MapperProfile;
import nto.core.entities.ServerEntity;
import org.springframework.stereotype.Component;

@Component // Важно! Чтобы Spring нашел этот бин и добавил в список в MappingServiceImpl
public class ServerProfile implements MapperProfile<ServerEntity, ServerDto> {

    @Override
    public Class<ServerEntity> getEntityClass() {
        return ServerEntity.class;
    }

    @Override
    public Class<ServerDto> getDtoClass() {
        return ServerDto.class;
    }

    @Override
    public ServerDto mapToDto(ServerEntity entity) {
        return new ServerDto(
                entity.getId(),
                entity.getHostname(),
                entity.getIpAddress(),
                entity.getPort()
        );
    }

    @Override
    public ServerEntity mapToEntity(ServerDto dto) {
        return ServerEntity.builder()
                .hostname(dto.hostname())
                .ipAddress(dto.ipAddress())
                .port(dto.port())
                .build();
    }

    @Override
    public void mapToEntity(ServerDto dto, ServerEntity entity) {
        entity.setHostname(dto.hostname());
        entity.setIpAddress(dto.ipAddress());
        entity.setPort(dto.port());
    }
}