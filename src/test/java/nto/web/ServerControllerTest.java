package nto.web;

import nto.application.dto.ServerDto;
import nto.application.interfaces.services.ServerService;
import nto.web.controllers.ServerController;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServerControllerTest {

    @Test
    void getAllShouldReturnAllServersWhenHostnameIsNull() {
        ServerService serverService = mock(ServerService.class);
        ServerController controller = new ServerController(serverService);
        List<ServerDto> servers = List.of(
            new ServerDto(1L, "srv-a", "10.0.0.1", 22, "root", "pw")
        );
        when(serverService.getAllServers()).thenReturn(servers);

        ResponseEntity<List<ServerDto>> response = controller.getAll(null);

        assertEquals(servers, response.getBody());
        verify(serverService).getAllServers();
    }

    @Test
    void getAllShouldFilterSafelyWhenHostnameIsProvided() {
        ServerService serverService = mock(ServerService.class);
        ServerController controller = new ServerController(serverService);
        List<ServerDto> servers = List.of(
            new ServerDto(1L, "alpha", "10.0.0.1", 22, "root", "pw"),
            new ServerDto(2L, null, "10.0.0.2", 22, "root", "pw"),
            new ServerDto(3L, "beta", "10.0.0.3", 22, "root", "pw")
        );
        when(serverService.getAllServers()).thenReturn(servers);

        ResponseEntity<List<ServerDto>> response = controller.getAll("alp");

        assertEquals(1, response.getBody().size());
        assertEquals("alpha", response.getBody().getFirst().hostname());
        verify(serverService).getAllServers();
    }
}
