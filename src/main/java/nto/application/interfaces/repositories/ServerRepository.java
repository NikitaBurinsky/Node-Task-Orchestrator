package nto.application.interfaces.repositories;

import nto.core.entities.ServerEntity;
import java.util.List;
import java.util.Optional;

public interface ServerRepository {
    Optional<ServerEntity> findById(Long id);
    List<ServerEntity> findAllByHostname(String hostname);
    List<ServerEntity> findAllById(Iterable<Long> ids);
    List<ServerEntity> findAllByOwnerUsername(String username);
}