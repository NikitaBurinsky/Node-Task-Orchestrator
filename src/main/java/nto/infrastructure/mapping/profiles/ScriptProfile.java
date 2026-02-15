package nto.infrastructure.mapping.profiles;

import lombok.RequiredArgsConstructor;
import nto.application.dto.ScriptDto;
import nto.application.interfaces.mapping.MapperProfile;
import nto.application.interfaces.repositories.UserRepository;
import nto.core.entities.ScriptEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScriptProfile implements MapperProfile<ScriptEntity, ScriptDto> {

    private final UserRepository userRepository;

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
                entity.getOwner() != null ? entity.getOwner().getId() : null
        );
    }

    @Override
    public ScriptEntity mapToEntity(ScriptDto dto) {
        var entity = ScriptEntity.builder()
                .name(dto.name())
                .content(dto.content())
                .build();

        resolveOwner(dto, entity);
        return entity;
    }

    @Override
    public void mapToEntity(ScriptDto dto, ScriptEntity entity) {
        entity.setName(dto.name());
        entity.setContent(dto.content());
        resolveOwner(dto, entity);
    }

    private void resolveOwner(ScriptDto dto, ScriptEntity entity) {
        if (dto.ownerId() != null) {
            entity.setOwner(userRepository.findById(dto.ownerId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + dto.ownerId())));
        }
    }
}