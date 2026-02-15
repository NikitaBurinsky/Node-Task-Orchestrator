package nto.infrastructure.repositories;

import nto.application.interfaces.repositories.ServerGroupRepository;
import nto.core.entities.ServerGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaServerGroupRepository extends JpaRepository<ServerGroupEntity, Long>, ServerGroupRepository {
}