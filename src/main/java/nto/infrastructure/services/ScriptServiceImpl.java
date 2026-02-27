package nto.infrastructure.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import nto.application.dto.ScriptDto;
import nto.application.interfaces.services.MappingService;
import nto.application.interfaces.services.ScriptService;
import nto.core.entities.ScriptEntity;
import nto.core.entities.UserEntity;
import nto.core.utils.ErrorMessages;
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

    @Transactional
    public ScriptDto createScript(ScriptDto dto) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        UserEntity currentUser = userRepository.findByUsername(username)
            .orElseThrow(
                () -> new EntityNotFoundException(ErrorMessages.USER_NOT_FOUND.getMessage()));

        ScriptEntity entity = mappingService.mapToEntity(dto, ScriptEntity.class);

        entity.setOwner(currentUser);

        ScriptEntity saved = scriptRepository.save(entity);

        return mappingService.mapToDto(saved, ScriptDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public ScriptDto getScriptById(Long id) {
        ScriptEntity script = scriptRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(
                ErrorMessages.SCRIPT_NOT_FOUND.getMessage() + " with id: " + id));

        return mappingService.mapToDto(script, ScriptDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScriptDto> getAllScripts() {
        List<ScriptEntity> scripts = scriptRepository.findAll();

        return mappingService.mapListToDto(scripts, ScriptDto.class);
    }

    @Override
    @Transactional
    public void deleteScript(Long id) {
        if (!scriptRepository.findById(id).isEmpty()) {
            throw new EntityNotFoundException(
                ErrorMessages.SCRIPT_NOT_FOUND.getMessage() + " with id: " + id);
        }
        scriptRepository.deleteById(id);
    }
}