package nto.infrastructure.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import nto.application.dto.ServerDto;
import nto.application.interfaces.services.MappingService;
import nto.application.interfaces.services.ScriptExecutor;
import nto.application.interfaces.services.ServerService;
import nto.core.entities.ServerEntity;
import nto.core.entities.ServerGroupEntity;
import nto.core.entities.SshUsernameEntity;
import nto.core.entities.UserEntity;
import nto.core.utils.ErrorMessages;
import nto.core.utils.ServerGroupDefaults;
import nto.infrastructure.cache.TaskStatusCache;
import nto.infrastructure.repositories.JpaServerGroupRepository;
import nto.infrastructure.repositories.JpaServerRepository;
import nto.infrastructure.repositories.JpaSshUsernameRepository;
import nto.infrastructure.repositories.JpaUserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServerServiceImpl implements ServerService {
    private final ScriptExecutor scriptExecutor;
    private final JpaServerRepository serverRepository;
    private final JpaServerGroupRepository groupRepository;
    private final MappingService mappingService;
    private final JpaUserRepository userRepository;
    private final JpaSshUsernameRepository sshUsernameRepository;
    private final TaskStatusCache tasksCache;

    @Override
    @Transactional(readOnly = true)
    public List<ServerDto> getAllServers() { 
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return mappingService.mapListToDto(
            serverRepository.findAllByOwnerUsername(username),
            ServerDto.class
        );
    }

    @Override
    @Transactional
    public void updateServer(Long id, ServerDto serverDto) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ServerEntity entity = serverRepository.findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException(ErrorMessages.SERVER_NOT_FOUND.getMessage()));
        ensureServerOwned(entity, username);
        mappingService.mapToEntity(serverDto, entity);
        if (serverDto.username() != null) {
            entity.setSshUsername(resolveSshUsername(getCurrentUser(username), serverDto.username()));
        }
        serverRepository.save(entity);
    }

    @Override
    @Transactional
    public void deleteServer(Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ServerEntity server = serverRepository.findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException(ErrorMessages.SERVER_NOT_FOUND.getMessage()));
        ensureServerOwned(server, username);
        tasksCache.evictAllByServerId(id);
        serverRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ServerDto getServerById(Long id) {
        ServerEntity server = serverRepository.findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException(ErrorMessages.SERVER_NOT_FOUND.getMessage()));
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ensureServerOwned(server, username);

        return mappingService.mapToDto(server, ServerDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServerDto> getServersByHostname(String hostname) {
        List<ServerEntity> servers;
        if (hostname == null || hostname.isBlank()) {
            return List.of();
        } else {
            servers = serverRepository.findAllByHostname(hostname);
        }

        return mappingService.mapListToDto(servers, ServerDto.class);
    }

    @Override
    @Transactional
    public ServerDto createServer(ServerDto dto) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        UserEntity currentUser = getCurrentUser(username);

        ServerEntity entity = mappingService.mapToEntity(dto, ServerEntity.class);

        entity.setSshUsername(resolveSshUsername(currentUser, dto.username()));
        ServerGroupEntity defaultGroup = getOrCreateDefaultGroup(currentUser);
        entity.getGroups().add(defaultGroup);
        defaultGroup.getServers().add(entity);

        ServerEntity saved = serverRepository.save(entity);

        return mappingService.mapToDto(saved, ServerDto.class);
    }

    @Override
    public boolean checkConnection(Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ServerEntity server = serverRepository.findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException(ErrorMessages.SERVER_NOT_FOUND.getMessage()));

        ensureServerOwned(server, username);
        return scriptExecutor.ping(id);
    }

    private UserEntity getCurrentUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException(ErrorMessages.USER_NOT_FOUND.getMessage()));
    }

    private ServerGroupEntity getOrCreateDefaultGroup(UserEntity user) {
        return groupRepository.findByOwnerUsernameAndName(user.getUsername(),
                ServerGroupDefaults.DEFAULT_GROUP_NAME)
            .orElseGet(() -> groupRepository.save(ServerGroupEntity.builder()
                .name(ServerGroupDefaults.DEFAULT_GROUP_NAME)
                .owner(user)
                .build()));
    }

    private void ensureServerOwned(ServerEntity server, String username) {
        boolean owned = server.getGroups().stream()
            .anyMatch(group -> group.getOwner() != null
                && username.equals(group.getOwner().getUsername()));
        if (!owned) {
            throw new AccessDeniedException(
                ErrorMessages.ACCESS_DENIED.getMessage() + ": You do not own this server");
        }
    }

    private SshUsernameEntity resolveSshUsername(UserEntity owner,
                                                 String rawUsername) {
        String username = rawUsername == null ? null : rawUsername.trim();
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("SSH username is required");
        }
        return sshUsernameRepository.findByOwnerAndUsername(owner, username)
            .orElseGet(() -> sshUsernameRepository.save(
                SshUsernameEntity.builder()
                    .owner(owner)
                    .username(username)
                    .build()
            ));
    }
}
