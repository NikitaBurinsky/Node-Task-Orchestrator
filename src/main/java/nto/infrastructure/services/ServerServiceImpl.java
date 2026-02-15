package nto.infrastructure.services;

import lombok.RequiredArgsConstructor;
import nto.application.dto.ServerDto;
import nto.application.interfaces.repositories.ServerRepository;
import nto.application.interfaces.services.ServerService;
import nto.core.entities.ServerEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // Lombok: генерирует конструктор для всех final полей (Constructor Injection)
public class ServerServiceImpl implements ServerService {

    private final ServerRepository serverRepository;

    @Override
    @Transactional(readOnly = true) // Hibernate optimization (аналог AsNoTracking в EF)
    public ServerDto getServerById(Long id) {
        ServerEntity server = serverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Server not found")); // Позже сделаем Custom Exception

        // Map From->To (Entity -> DTO)
        return new ServerDto(server.getId(), server.getHostname(), server.getIpAddress(), server.getPort());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServerDto> getServersByHostname(String hostname) {
        List<ServerEntity> servers;
        if (hostname == null || hostname.isBlank()) {
            // В реальном JpaRepo findAll() возвращает все, но у нас в интерфейсе его нет, надо добавить если нужно
            // Для примера предположим, что если hostname пуст, мы ничего не ищем или ищем все (нужен доп метод)
            // Допустим, ищем конкретно
            return List.of();
        } else {
            servers = serverRepository.findAllByHostname(hostname);
        }

        return servers.stream()
                .map(server -> {
                    // Map From->To (Entity -> DTO)
                    return new ServerDto(server.getId(), server.getHostname(), server.getIpAddress(), server.getPort());
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional // Открывает транзакцию на запись
    public ServerDto createServer(ServerDto dto) {
        // Map From->To (DTO -> Entity)
        ServerEntity entity = ServerEntity.builder()
                .hostname(dto.hostname())
                .ipAddress(dto.ipAddress())
                .port(dto.port())
                .build();

        ServerEntity saved = serverRepository.save(entity);

        // Map From->To (Entity -> DTO)
        return new ServerDto(saved.getId(), saved.getHostname(), saved.getIpAddress(), saved.getPort());
    }
}