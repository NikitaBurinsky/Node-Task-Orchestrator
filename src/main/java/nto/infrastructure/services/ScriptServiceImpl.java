package nto.infrastructure.services;

import lombok.RequiredArgsConstructor;
import nto.application.dto.ScriptDto;
import nto.application.interfaces.repositories.ScriptRepository;
import nto.application.interfaces.services.MappingService;
import nto.application.interfaces.services.ScriptService;
import nto.core.entities.ScriptEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScriptServiceImpl implements ScriptService {

    private final ScriptRepository scriptRepository;
    private final MappingService mappingService;

    @Override
    @Transactional
    public ScriptDto createScript(ScriptDto dto) {
        // 1. Map Dto -> Entity (Profile сам подтянет Owner по ID)
        ScriptEntity entity = mappingService.mapToEntity(dto, ScriptEntity.class);

        // 2. Save
        ScriptEntity saved = scriptRepository.save(entity);

        // 3. Map Entity -> Dto
        return mappingService.mapToDto(saved, ScriptDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public ScriptDto getScriptById(Long id) {
        ScriptEntity script = scriptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Script not found with id: " + id));

        return mappingService.mapToDto(script, ScriptDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScriptDto> getAllScripts() {
        // Здесь можно добавить пагинацию в будущем
        List<ScriptEntity> scripts = ((org.springframework.data.jpa.repository.JpaRepository<ScriptEntity, Long>) scriptRepository).findAll();

        return mappingService.mapListToDto(scripts, ScriptDto.class);
    }

    @Override
    @Transactional
    public void deleteScript(Long id) {
        if (!scriptRepository.findById(id).isPresent()) {
            throw new RuntimeException("Script not found with id: " + id);
        }
        //TODO
        ((org.springframework.data.jpa.repository.JpaRepository<ScriptEntity, Long>) scriptRepository).deleteById(id);
    }
}