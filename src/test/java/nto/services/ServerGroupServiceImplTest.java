package nto.services;

import nto.application.interfaces.services.MappingService;
import nto.application.interfaces.services.ScriptExecutor;
import nto.core.entities.ServerEntity;
import nto.core.entities.ServerGroupEntity;
import nto.core.entities.UserEntity;
import nto.infrastructure.cache.TaskStatusCache;
import nto.infrastructure.repositories.JpaScriptRepository;
import nto.infrastructure.repositories.JpaServerGroupRepository;
import nto.infrastructure.repositories.JpaServerRepository;
import nto.infrastructure.repositories.JpaTaskRepository;
import nto.infrastructure.repositories.JpaUserRepository;
import nto.infrastructure.services.ServerGroupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServerGroupServiceImplTest {

    @Mock private JpaServerGroupRepository groupRepository;
    @Mock private JpaServerRepository serverRepository;
    @Mock private JpaUserRepository userRepository;
    @Mock private JpaScriptRepository scriptRepository;
    @Mock private JpaTaskRepository taskRepository;
    @Mock private MappingService mappingService;
    @Mock private ScriptExecutor scriptExecutor;
    @Mock private TaskStatusCache statusCache;

    @InjectMocks
    private ServerGroupServiceImpl groupService;

    private final String TEST_USERNAME = "admin";

    @BeforeEach
    void setUpSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(TEST_USERNAME);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void pingGroup_ShouldReturnMapOfStatuses() {
        // Подготовка данных
        Long groupId = 1L;

        UserEntity owner = new UserEntity();
        owner.setUsername(TEST_USERNAME);

        ServerEntity server1 = new ServerEntity();
        server1.setId(10L);
        ServerEntity server2 = new ServerEntity();
        server2.setId(20L);

        ServerGroupEntity group = new ServerGroupEntity();
        group.setId(groupId);
        group.setOwner(owner);
        group.setServers(Set.of(server1, server2));

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(scriptExecutor.ping(10L)).thenReturn(true);
        when(scriptExecutor.ping(20L)).thenReturn(false);

        Map<Long, Boolean> results = groupService.pingGroup(groupId);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.get(10L), "Сервер 10 должен быть доступен");
        assertFalse(results.get(20L), "Сервер 20 должен быть недоступен");
    }
}