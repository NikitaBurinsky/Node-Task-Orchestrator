package nto.infrastructure.services;

import lombok.RequiredArgsConstructor;
import nto.application.dto.ServerDto;
import nto.application.interfaces.repositories.ServerRepository;
import nto.application.interfaces.services.MappingService;
import nto.application.interfaces.services.ServerService;
import nto.core.entities.ServerEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class ServerServiceImpl implements ServerService {

    private final ServerRepository serverRepository;
    private final MappingService mappingService;

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
        ServerEntity entity = mappingService.mapToEntity(dto, ServerEntity.class);

        ServerEntity saved = serverRepository.save(entity);

        return mappingService.mapToDto(saved, ServerDto.class);
    }
}