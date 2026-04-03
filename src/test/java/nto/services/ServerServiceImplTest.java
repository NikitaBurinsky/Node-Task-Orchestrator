package nto.services;

import jakarta.persistence.EntityNotFoundException;
import nto.application.dto.ServerDto;
import nto.application.interfaces.services.MappingService;
import nto.application.interfaces.services.ScriptExecutor;
import nto.core.entities.ServerEntity;
import nto.core.entities.ServerGroupEntity;
import nto.core.entities.SshUsernameEntity;
import nto.core.entities.UserEntity;
import nto.core.utils.ServerGroupDefaults;
import nto.core.utils.exceptions.BadRequestException;
import nto.infrastructure.cache.TaskStatusCache;
import nto.infrastructure.repositories.JpaServerGroupRepository;
import nto.infrastructure.repositories.JpaServerRepository;
import nto.infrastructure.repositories.JpaSshUsernameRepository;
import nto.infrastructure.repositories.JpaUserRepository;
import nto.infrastructure.services.ServerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServerServiceImplTest {

    private static final String TEST_USER = "admin";

    @Mock
    private ScriptExecutor scriptExecutor;
    @Mock
    private JpaServerRepository serverRepository;
    @Mock
    private JpaServerGroupRepository groupRepository;
    @Mock
    private MappingService mappingService;
    @Mock
    private JpaUserRepository userRepository;
    @Mock
    private JpaSshUsernameRepository sshUsernameRepository;
    @Mock
    private TaskStatusCache tasksCache;

    @InjectMocks
    private ServerServiceImpl serverService;

    @BeforeEach
    void setUpSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn(TEST_USER);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getAllServersShouldMapOwnedServers() {
        List<ServerEntity> entities = List.of(serverOwnedBy(TEST_USER), serverOwnedBy(TEST_USER));
        List<ServerDto> expected = List.of(
            new ServerDto(1L, "srv1", "10.0.0.1", 22, "root", "pw"),
            new ServerDto(2L, "srv2", "10.0.0.2", 22, "root", "pw")
        );

        when(serverRepository.findAllByOwnerUsername(TEST_USER)).thenReturn(entities);
        when(mappingService.mapListToDto(entities, ServerDto.class)).thenReturn(expected);

        List<ServerDto> result = serverService.getAllServers();

        assertEquals(expected, result);
    }

    @Test
    void updateServerShouldThrowWhenServerIsMissing() {
        when(serverRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
            () -> serverService.updateServer(1L, new ServerDto(null, "s", "10.0.0.1", 22, "root", "pw")));
    }

    @Test
    void updateServerShouldThrowWhenServerNotOwned() {
        ServerEntity server = serverOwnedBy("other");
        server.setId(1L);

        when(serverRepository.findById(1L)).thenReturn(Optional.of(server));

        assertThrows(AccessDeniedException.class,
            () -> serverService.updateServer(1L, new ServerDto(null, "s", "10.0.0.1", 22, "root", "pw")));
    }

    @Test
    void updateServerShouldResolveAndSaveSshUsername() {
        ServerEntity server = serverOwnedBy(TEST_USER);
        server.setId(1L);
        UserEntity owner = UserEntity.builder().id(7L).username(TEST_USER).build();
        SshUsernameEntity ssh = SshUsernameEntity.builder().id(8L).owner(owner).username("root").build();
        ServerDto dto = new ServerDto(null, "updated", "10.0.0.3", 2222, "root", "newpw");

        when(serverRepository.findById(1L)).thenReturn(Optional.of(server));
        when(userRepository.findByUsername(TEST_USER)).thenReturn(Optional.of(owner));
        when(sshUsernameRepository.findByOwnerAndUsername(owner, "root")).thenReturn(Optional.of(ssh));

        serverService.updateServer(1L, dto);

        assertEquals(ssh, server.getSshUsername());
        verify(serverRepository).save(server);
    }

    @Test
    void updateServerShouldThrowWhenSshUsernameBlank() {
        ServerEntity server = serverOwnedBy(TEST_USER);
        server.setId(1L);
        UserEntity owner = UserEntity.builder().id(7L).username(TEST_USER).build();
        ServerDto dto = new ServerDto(null, "updated", "10.0.0.3", 2222, " ", "newpw");

        when(serverRepository.findById(1L)).thenReturn(Optional.of(server));
        when(userRepository.findByUsername(TEST_USER)).thenReturn(Optional.of(owner));

        assertThrows(BadRequestException.class, () -> serverService.updateServer(1L, dto));
        verify(serverRepository, never()).save(any(ServerEntity.class));
    }

    @Test
    void deleteServerShouldEvictCacheAndDelete() {
        ServerEntity server = serverOwnedBy(TEST_USER);
        server.setId(5L);
        when(serverRepository.findById(5L)).thenReturn(Optional.of(server));

        serverService.deleteServer(5L);

        verify(tasksCache).evictAllByServerId(5L);
        verify(serverRepository).deleteById(5L);
    }

    @Test
    void deleteServerShouldThrowWhenNotOwned() {
        ServerEntity server = serverOwnedBy("other");
        server.setId(5L);
        when(serverRepository.findById(5L)).thenReturn(Optional.of(server));

        assertThrows(AccessDeniedException.class, () -> serverService.deleteServer(5L));
        verify(serverRepository, never()).deleteById(5L);
    }

    @Test
    void getServerByIdShouldMapWhenOwned() {
        ServerEntity server = serverOwnedBy(TEST_USER);
        server.setId(10L);
        ServerDto expected = new ServerDto(10L, "srv", "10.0.0.1", 22, "root", "pw");

        when(serverRepository.findById(10L)).thenReturn(Optional.of(server));
        when(mappingService.mapToDto(server, ServerDto.class)).thenReturn(expected);

        ServerDto result = serverService.getServerById(10L);

        assertEquals(expected, result);
    }

    @Test
    void getServerByIdShouldThrowWhenNotOwned() {
        ServerEntity server = serverOwnedBy("other");
        server.setId(10L);
        when(serverRepository.findById(10L)).thenReturn(Optional.of(server));

        assertThrows(AccessDeniedException.class, () -> serverService.getServerById(10L));
    }

    @Test
    void getServersByHostnameShouldReturnEmptyOnBlankInput() {
        List<ServerDto> result = serverService.getServersByHostname(" ");

        assertTrue(result.isEmpty());
        verify(serverRepository, never()).findAllByHostname(any());
    }

    @Test
    void getServersByHostnameShouldMapRepositoryResult() {
        List<ServerEntity> entities = List.of(serverOwnedBy(TEST_USER));
        List<ServerDto> expected = List.of(new ServerDto(1L, "srv", "10.0.0.1", 22, "root", "pw"));

        when(serverRepository.findAllByHostname("srv")).thenReturn(entities);
        when(mappingService.mapListToDto(entities, ServerDto.class)).thenReturn(expected);

        List<ServerDto> result = serverService.getServersByHostname("srv");

        assertEquals(expected, result);
    }

    @Test
    void createServerShouldAttachExistingDefaultGroupAndMapDto() {
        UserEntity owner = UserEntity.builder().id(1L).username(TEST_USER).build();
        ServerGroupEntity defaultGroup = ServerGroupEntity.builder()
            .id(99L)
            .name(ServerGroupDefaults.DEFAULT_GROUP_NAME)
            .owner(owner)
            .servers(new HashSet<>())
            .build();
        SshUsernameEntity ssh = SshUsernameEntity.builder().id(8L).owner(owner).username("root").build();
        ServerEntity mapped = ServerEntity.builder().groups(new HashSet<>()).build();
        ServerEntity saved = ServerEntity.builder().id(44L).groups(new HashSet<>()).build();
        ServerDto input = new ServerDto(null, "srv", "10.0.0.1", 22, "root", "pw");
        ServerDto expected = new ServerDto(44L, "srv", "10.0.0.1", 22, "root", "pw");

        when(userRepository.findByUsername(TEST_USER)).thenReturn(Optional.of(owner));
        when(mappingService.mapToEntity(input, ServerEntity.class)).thenReturn(mapped);
        when(sshUsernameRepository.findByOwnerAndUsername(owner, "root")).thenReturn(Optional.of(ssh));
        when(groupRepository.findByOwnerUsernameAndName(TEST_USER, ServerGroupDefaults.DEFAULT_GROUP_NAME))
            .thenReturn(Optional.of(defaultGroup));
        when(serverRepository.save(mapped)).thenReturn(saved);
        when(mappingService.mapToDto(saved, ServerDto.class)).thenReturn(expected);

        ServerDto result = serverService.createServer(input);

        assertEquals(expected, result);
        assertEquals(ssh, mapped.getSshUsername());
        assertTrue(mapped.getGroups().contains(defaultGroup));
        assertTrue(defaultGroup.getServers().contains(mapped));
    }

    @Test
    void createServerShouldCreateDefaultGroupAndSshUsernameWhenMissing() {
        UserEntity owner = UserEntity.builder().id(1L).username(TEST_USER).build();
        ServerGroupEntity newGroup = ServerGroupEntity.builder()
            .id(99L)
            .name(ServerGroupDefaults.DEFAULT_GROUP_NAME)
            .owner(owner)
            .servers(new HashSet<>())
            .build();
        SshUsernameEntity newSsh = SshUsernameEntity.builder().id(8L).owner(owner).username("root").build();
        ServerEntity mapped = ServerEntity.builder().groups(new HashSet<>()).build();
        ServerEntity saved = ServerEntity.builder().id(44L).groups(new HashSet<>()).build();
        ServerDto input = new ServerDto(null, "srv", "10.0.0.1", 22, "root", "pw");

        when(userRepository.findByUsername(TEST_USER)).thenReturn(Optional.of(owner));
        when(mappingService.mapToEntity(input, ServerEntity.class)).thenReturn(mapped);
        when(sshUsernameRepository.findByOwnerAndUsername(owner, "root")).thenReturn(Optional.empty());
        when(sshUsernameRepository.save(any(SshUsernameEntity.class))).thenReturn(newSsh);
        when(groupRepository.findByOwnerUsernameAndName(TEST_USER, ServerGroupDefaults.DEFAULT_GROUP_NAME))
            .thenReturn(Optional.empty());
        when(groupRepository.save(any(ServerGroupEntity.class))).thenReturn(newGroup);
        when(serverRepository.save(mapped)).thenReturn(saved);
        when(mappingService.mapToDto(saved, ServerDto.class)).thenReturn(input);

        serverService.createServer(input);

        verify(groupRepository).save(any(ServerGroupEntity.class));
        verify(sshUsernameRepository).save(any(SshUsernameEntity.class));
    }

    @Test
    void checkConnectionShouldReturnPingResult() {
        ServerEntity server = serverOwnedBy(TEST_USER);
        server.setId(50L);

        when(serverRepository.findById(50L)).thenReturn(Optional.of(server));
        when(scriptExecutor.ping(50L)).thenReturn(true);

        boolean result = serverService.checkConnection(50L);

        assertTrue(result);
        verify(scriptExecutor).ping(50L);
    }

    @Test
    void checkConnectionShouldThrowWhenServerNotOwned() {
        ServerEntity server = serverOwnedBy("other");
        server.setId(50L);
        when(serverRepository.findById(50L)).thenReturn(Optional.of(server));

        assertThrows(AccessDeniedException.class, () -> serverService.checkConnection(50L));
        verify(scriptExecutor, never()).ping(any());
    }

    private ServerEntity serverOwnedBy(String username) {
        UserEntity owner = UserEntity.builder().id(99L).username(username).build();
        ServerGroupEntity group = ServerGroupEntity.builder()
            .id(100L)
            .name("g")
            .owner(owner)
            .servers(new HashSet<>())
            .build();

        return ServerEntity.builder()
            .id(1L)
            .hostname("srv")
            .ipAddress("10.0.0.1")
            .port(22)
            .password("pw")
            .groups(new HashSet<>(List.of(group)))
            .build();
    }
}
