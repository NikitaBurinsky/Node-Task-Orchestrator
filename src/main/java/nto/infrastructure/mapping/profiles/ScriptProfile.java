package nto.infrastructure.mapping.profiles;

import lombok.RequiredArgsConstructor;
import nto.application.dto.ScriptDto;
import nto.application.interfaces.mapping.MapperProfile;
import nto.core.entities.ScriptEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScriptProfile implements MapperProfile<ScriptEntity, ScriptDto> {

    @Override
    public Class<ScriptEntity> getEntityClass() {
        return ScriptEntity.class;
    }

    @Override
    public Class<ScriptDto> getDtoClass() {
        return ScriptDto.class;
    }

    @Override
    public ScriptDto mapToDto(ScriptEntity entity) {
        return new ScriptDto(
            entity.getId(),
            entity.getName(),
            entity.getContent(),
            // Безопасно достаем имя владельца
            entity.getOwner() != null ? entity.getOwner().getUsername() : null,
            entity.getIsPublic()
        );
    }

    @Override
    public ScriptEntity mapToEntity(ScriptDto dto) {
        // При создании из DTO мы игнорируем ownerName,
        // так как владелец будет установлен в Service из SecurityContext
        return ScriptEntity.builder()
            .name(dto.name())
            .content(dto.content())
            .isPublic(dto.isPublic() != null && dto.isPublic())
            .build();
    }

    @Override
    public void mapToEntity(ScriptDto dto, ScriptEntity entity) {
        entity.setName(dto.name());
        entity.setContent(dto.content());
        if (dto.isPublic() != null) {
            entity.setIsPublic(dto.isPublic());
        }
        // Owner здесь не обновляем
    }
}