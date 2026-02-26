package nto.infrastructure.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import nto.application.dto.ServerDto;
import nto.application.interfaces.services.MappingService;
import nto.application.interfaces.services.ScriptExecutor;
import nto.application.interfaces.services.ServerService;
import nto.core.entities.ServerEntity;
import nto.core.utils.ErrorMessages;
import nto.infrastructure.cache.TaskStatusCache;
import nto.infrastructure.repositories.JpaServerRepository;
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
    private final MappingService mappingService;
    private final JpaUserRepository userRepository;
    private final TaskStatusCache taskStatusCache;

    @Override
    @Transactional(readOnly = true)
    public List<ServerDto> getAllServers() { // Нужно добавить этот метод в интерфейс
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return mappingService.mapListToDto(
            serverRepository.findAllByOwnerUsername(username),
            ServerDto.class
        );
    }

    @Override
    @Transactional
    public void updateServer(Long id, ServerDto serverDto) {
        ServerEntity entity = serverRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(ErrorMessages.SERVER_NOT_FOUND.getMessage()));
        mappingService.mapToEntity(serverDto, entity);
        serverRepository.save(entity);
    }

    @Override
    @Transactional
    public void deleteServer(Long id) {
        if (!serverRepository.existsById(id)) {
            throw new EntityNotFoundException(ErrorMessages.SERVER_NOT_FOUND.getMessage());
        }
        taskStatusCache.evictAllByServerId(id);
        serverRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ServerDto getServerById(Long id) {
        ServerEntity server = serverRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(ErrorMessages.SERVER_NOT_FOUND.getMessage()));

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

        nto.core.entities.UserEntity currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

        ServerEntity entity = mappingService.mapToEntity(dto, ServerEntity.class);

        // Привязываем владельца
        entity.setOwner(currentUser);

        ServerEntity saved = serverRepository.save(entity);

        return mappingService.mapToDto(saved, ServerDto.class);
    }

    @Override
    public boolean checkConnection(Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ServerEntity server = serverRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(ErrorMessages.SERVER_NOT_FOUND.getMessage()));

        if (!server.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException(ErrorMessages.ACCESS_DENIED.getMessage() + ": You do not own this server");
        }
        return scriptExecutor.ping(id);
    }
}