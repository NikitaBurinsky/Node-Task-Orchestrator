package nto.infrastructure.repositories;

import nto.application.interfaces.repositories.ServerGroupRepository;
import nto.core.entities.ServerGroupEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaServerGroupRepository extends JpaRepository<ServerGroupEntity, Long>,
    ServerGroupRepository {

    // Подгружаем серверы сразу, чтобы избежать N+1 при маппинге DTO
    @EntityGraph(attributePaths = {"servers", "owner"})
    Optional<ServerGroupEntity> findById(Long id);

    @EntityGraph(attributePaths = {"servers", "owner"})
    List<ServerGroupEntity> findAllByOwnerUsername(String username);
}