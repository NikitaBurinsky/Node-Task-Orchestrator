package nto.infrastructure.mapping.profiles;

import lombok.RequiredArgsConstructor;
import nto.application.dto.TaskDto;
import nto.application.interfaces.mapping.MapperProfile;
import nto.application.interfaces.repositories.ScriptRepository;
import nto.application.interfaces.repositories.ServerRepository;
import nto.core.entities.TaskEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskProfile implements MapperProfile<TaskEntity, TaskDto> {

    private final ServerRepository serverRepository;
    private final ScriptRepository scriptRepository;

    @Override
    public Class<TaskEntity> getEntityClass() {
        return TaskEntity.class;
    }

    @Override
    public Class<TaskDto> getDtoClass() {
        return TaskDto.class;
    }

    @Override
    public TaskDto mapToDto(TaskEntity entity) {
        return new TaskDto(
                entity.getId(),
                entity.getStatus(),
                entity.getOutput(),
                entity.getServer().getId(),
                entity.getScript().getId(),
                // Маппим ID группы, если она есть
                entity.getSourceGroup() != null ? entity.getSourceGroup().getId() : null,
                entity.getStartedAt(),
                entity.getFinishedAt()
        );
    }

    @Override
    public TaskEntity mapToEntity(TaskDto dto) {
        var entity = TaskEntity.builder()
                .status(dto.status())
                .output(dto.output())
                .build();

        // Разрешаем связи
        resolveReferences(dto, entity);

        return entity;
    }

    @Override
    public void mapToEntity(TaskDto dto, TaskEntity entity) {
        entity.setStatus(dto.status());
        entity.setOutput(dto.output());
        resolveReferences(dto, entity);
    }

    private void resolveReferences(TaskDto dto, TaskEntity entity) {
        if (dto.serverId() != null) {
            entity.setServer(serverRepository.findById(dto.serverId())
                    .orElseThrow(() -> new RuntimeException("Server not found: " + dto.serverId())));
        }
        if (dto.scriptId() != null) {
            entity.setScript(scriptRepository.findById(dto.scriptId())
                    .orElseThrow(() -> new RuntimeException("Script not found: " + dto.scriptId())));
        }
    }
}