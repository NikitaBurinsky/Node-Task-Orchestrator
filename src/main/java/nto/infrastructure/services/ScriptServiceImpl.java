package nto.infrastructure.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import nto.application.dto.ScriptDto;
import nto.application.interfaces.services.MappingService;
import nto.application.interfaces.services.ScriptService;
import nto.core.entities.ScriptEntity;
import nto.core.entities.UserEntity;
import nto.infrastructure.repositories.JpaScriptRepository;
import nto.infrastructure.repositories.JpaUserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScriptServiceImpl implements ScriptService {
    private final JpaUserRepository userRepository;
    private final JpaScriptRepository scriptRepository;
    private final MappingService mappingService;


    @Transactional(readOnly = true)
    public List<ScriptDto> getAllAvailableScripts() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // Используем наш кастомный запрос
        List<ScriptEntity> scripts = scriptRepository.findAllAvailableForUser(username);
        return mappingService.mapListToDto(scripts, ScriptDto.class);
    }

    @Transactional
    public ScriptDto createScript(ScriptDto dto) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        UserEntity currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found in DB"));

        ScriptEntity entity = mappingService.mapToEntity(dto, ScriptEntity.class);

        entity.setOwner(currentUser);

        ScriptEntity saved = scriptRepository.save(entity);

        return mappingService.mapToDto(saved, ScriptDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public ScriptDto getScriptById(Long id) {
        ScriptEntity script = scriptRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Script not found with id: " + id));

        return mappingService.mapToDto(script, ScriptDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScriptDto> getAllScripts() {
        // Здесь можно добавить пагинацию в будущем
        List<ScriptEntity> scripts = scriptRepository.findAll();

        return mappingService.mapListToDto(scripts, ScriptDto.class);
    }

    @Override
    @Transactional
    public void deleteScript(Long id) {
        if (!scriptRepository.findById(id).isPresent()) {
            throw new EntityNotFoundException("Script not found with id: " + id);
        }
        //TODO
        scriptRepository.deleteById(
            id);
    }
}