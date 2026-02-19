package nto.infrastructure.mapping.profiles;

import nto.application.dto.UserDto;
import nto.application.interfaces.mapping.MapperProfile;
import nto.core.entities.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserProfile implements MapperProfile<UserEntity, UserDto> {

    @Override
    public Class<UserEntity> getEntityClass() {
        return UserEntity.class;
    }

    @Override
    public Class<UserDto> getDtoClass() {
        return UserDto.class;
    }

    @Override
    public UserDto mapToDto(UserEntity entity) {
        return new UserDto(
            entity.getId(),
            entity.getUsername(),
            entity.getPassword()
        );
    }

    @Override
    public UserEntity mapToEntity(UserDto dto) {
        return UserEntity.builder()
            .username(dto.username())
            .password(dto.password())
            .build();
    }

    @Override
    public void mapToEntity(UserDto dto, UserEntity entity) {
        entity.setUsername(dto.username());
        if (dto.password() != null && !dto.password().isBlank()) {
            entity.setPassword(dto.password());
        }
    }
}