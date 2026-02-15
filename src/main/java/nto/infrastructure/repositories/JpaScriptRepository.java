package nto.infrastructure.repositories;

import nto.application.interfaces.repositories.ScriptRepository;
import nto.core.entities.ScriptEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaScriptRepository extends JpaRepository<ScriptEntity, Long>, ScriptRepository {

    @Override
    @EntityGraph(attributePaths = {"owner"}) // Жадная загрузка владельца скрипта
    Optional<ScriptEntity> findById(Long id);
}