package nto.services;

import jakarta.persistence.EntityNotFoundException;
import nto.application.dto.ScriptDto;
import nto.application.interfaces.services.MappingService;
import nto.core.entities.ScriptEntity;
import nto.core.entities.UserEntity;
import nto.infrastructure.repositories.JpaScriptRepository;
import nto.infrastructure.repositories.JpaUserRepository;
import nto.infrastructure.services.ScriptServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScriptServiceImplTest {

    private static final String TEST_USER = "admin";

    @Mock
    private JpaUserRepository userRepository;
    @Mock
    private JpaScriptRepository scriptRepository;
    @Mock
    private MappingService mappingService;

    @InjectMocks
    private ScriptServiceImpl scriptService;

    @BeforeEach
    void setUpSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn(TEST_USER);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createScriptShouldPersistWithCurrentOwner() {
        ScriptDto input = new ScriptDto(null, "deploy", "echo ok", null, false);
        UserEntity owner = UserEntity.builder().id(1L).username(TEST_USER).build();
        ScriptEntity mapped = ScriptEntity.builder().name("deploy").content("echo ok").build();
        ScriptEntity saved = ScriptEntity.builder()
            .id(10L)
            .name("deploy")
            .content("echo ok")
            .owner(owner)
            .build();
        ScriptDto expected = new ScriptDto(10L, "deploy", "echo ok", TEST_USER, false);

        when(userRepository.findByUsername(TEST_USER)).thenReturn(Optional.of(owner));
        when(mappingService.mapToEntity(input, ScriptEntity.class)).thenReturn(mapped);
        when(scriptRepository.save(mapped)).thenReturn(saved);
        when(mappingService.mapToDto(saved, ScriptDto.class)).thenReturn(expected);

        ScriptDto result = scriptService.createScript(input);

        assertEquals(expected, result);
        assertEquals(owner, mapped.getOwner());
    }

    @Test
    void createScriptShouldThrowWhenUserNotFound() {
        ScriptDto input = new ScriptDto(null, "deploy", "echo ok", null, false);
        when(userRepository.findByUsername(TEST_USER)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> scriptService.createScript(input));
    }

    @Test
    void getScriptByIdShouldMapEntity() {
        ScriptEntity script = ScriptEntity.builder().id(10L).name("deploy").content("echo").build();
        ScriptDto expected = new ScriptDto(10L, "deploy", "echo", TEST_USER, false);

        when(scriptRepository.findById(10L)).thenReturn(Optional.of(script));
        when(mappingService.mapToDto(script, ScriptDto.class)).thenReturn(expected);

        ScriptDto result = scriptService.getScriptById(10L);

        assertEquals(expected, result);
    }

    @Test
    void getScriptByIdShouldThrowWhenMissing() {
        when(scriptRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> scriptService.getScriptById(10L));
    }

    @Test
    void getAllScriptsShouldMapRepositoryResult() {
        List<ScriptEntity> scripts = List.of(
            ScriptEntity.builder().id(1L).name("a").build(),
            ScriptEntity.builder().id(2L).name("b").build()
        );
        List<ScriptDto> expected = List.of(
            new ScriptDto(1L, "a", "", TEST_USER, false),
            new ScriptDto(2L, "b", "", TEST_USER, true)
        );

        when(scriptRepository.findAll()).thenReturn(scripts);
        when(mappingService.mapListToDto(scripts, ScriptDto.class)).thenReturn(expected);

        List<ScriptDto> result = scriptService.getAllScripts();

        assertEquals(expected, result);
    }

    @Test
    void deleteScriptShouldThrowWhenNotFound() {
        when(scriptRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> scriptService.deleteScript(5L));
    }

    @Test
    void deleteScriptShouldDeleteWhenExists() {
        when(scriptRepository.findById(5L)).thenReturn(Optional.of(ScriptEntity.builder().id(5L).build()));

        scriptService.deleteScript(5L);

        verify(scriptRepository).deleteById(5L);
    }
}
