package nto.web.controllers;

import lombok.RequiredArgsConstructor;
import nto.application.dto.ServerDto;
import nto.application.interfaces.services.ServerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class ServerController {

    private final ServerService serverService;

    @PostMapping
    public ResponseEntity<ServerDto> create(@RequestBody ServerDto dto) {
        return ResponseEntity.ok(serverService.createServer(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServerDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(serverService.getServerById(id));
    }

    // GET /api/servers?hostname=srv-01
    @GetMapping
    public ResponseEntity<List<ServerDto>> getByHostname(@RequestParam(required = false) String hostname) {
        return ResponseEntity.ok(serverService.getServersByHostname(hostname));
    }
}