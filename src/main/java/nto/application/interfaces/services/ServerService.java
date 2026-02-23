// Порт для Сервиса (Input Port)
package nto.application.interfaces.services;

import nto.application.dto.ServerDto;

import java.util.List;

public interface ServerService {
    ServerDto getServerById(Long id);

    List<ServerDto> getServersByHostname(String hostname);

    ServerDto createServer(ServerDto dto);

    boolean checkConnection(Long id);

    List<ServerDto> getAllServers();

    void updateServer(Long id, ServerDto serverDto);

    void deleteServer(Long id);
}