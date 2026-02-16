package nto.infrastructure.repositories;

import nto.application.interfaces.repositories.ServerRepository;
import nto.application.interfaces.services.ServerService;
import nto.core.entities.ServerEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaServerRepository extends JpaRepository<ServerEntity, Long>, ServerRepository{

    @EntityGraph(attributePaths = {"groups"})
    List<ServerEntity> findAllByHostname(String hostname);

    @EntityGraph(attributePaths = {"groups"})
    List<ServerEntity> findAllById(Iterable<Long> ids);

    // Пример решения N+1
    // attributePaths указывает поля, которые нужно "сджойнить" (LEFT OUTER JOIN)
    @EntityGraph(attributePaths = {"groups"})
    List<ServerEntity> findAll();

    // Можно переопределить стандартный findById, чтобы сразу тянуть владельца
    @Override
    @EntityGraph(attributePaths = {"owner", "groups"})
    Optional<ServerEntity> findById(Long id);

}