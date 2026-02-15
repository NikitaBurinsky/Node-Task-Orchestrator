package nto.application.interfaces.repositories;

import nto.core.entities.ServerEntity;
import java.util.List;
import java.util.Optional;

public interface ServerRepository {
    ServerEntity save(ServerEntity server);
    Optional<ServerEntity> findById(Long id); // Optional ≈ Nullable reference types
    List<ServerEntity> findAllByHostname(String hostname);
}