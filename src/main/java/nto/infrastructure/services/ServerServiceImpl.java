package nto.infrastructure.services;

import lombok.RequiredArgsConstructor;
import nto.application.dto.ServerDto;
import nto.application.interfaces.repositories.ServerRepository;
import nto.application.interfaces.services.MappingService;
import nto.application.interfaces.services.ScriptExecutor;
import nto.application.interfaces.services.ServerService;
import nto.core.entities.ServerEntity;
import nto.infrastructure.repositories.JpaUserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class ServerServiceImpl implements ServerService {
    private final ScriptExecutor scriptExecutor;
    private final ServerRepository serverRepository;
    private final MappingService mappingService;
    private final JpaUserRepository userRepository;

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
    @Transactional(readOnly = true)
    public ServerDto getServerById(Long id) {
        ServerEntity server = serverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Server not found"));

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
        // Получаем текущего юзера
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();

        nto.core.entities.UserEntity currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

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
                .orElseThrow(() -> new RuntimeException("Server not found"));

        if (!server.getOwner().getUsername().equals(username)) {
            throw new RuntimeException("Access Denied: You do not own this server");
        }
        return scriptExecutor.ping(id);
    }
}