package nto.application.interfaces.repositories;

import nto.core.entities.ServerGroupEntity;
import java.util.Optional;

public interface ServerGroupRepository {
    Optional<ServerGroupEntity> findById(Long id);
}