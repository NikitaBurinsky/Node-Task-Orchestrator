package nto.infrastructure.repositories;

import nto.application.interfaces.repositories.ServerRepository;
import nto.core.entities.ServerEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaServerRepository extends JpaRepository<ServerEntity, Long>, ServerRepository {

    @EntityGraph(attributePaths = {"groups", "sshUsername"})
    List<ServerEntity> findAllByHostname(String hostname);

    @EntityGraph(attributePaths = {"groups", "sshUsername"})
    List<ServerEntity> findAllById(Iterable<Long> ids);

    @EntityGraph(attributePaths = {"groups", "sshUsername"})
    List<ServerEntity> findAll();

    @Override
    @EntityGraph(attributePaths = {"groups", "groups.owner", "sshUsername"})
    Optional<ServerEntity> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"groups", "sshUsername"})
    @Query("SELECT DISTINCT s FROM ServerEntity s JOIN s.groups g WHERE g.owner.username = :username")
    List<ServerEntity> findAllByOwnerUsername(@Param("username") String username);
}
