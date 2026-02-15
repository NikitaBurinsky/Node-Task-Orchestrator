package nto.application.interfaces.repositories;

import nto.core.entities.ScriptEntity;
import java.util.Optional;

public interface ScriptRepository {
    Optional<ScriptEntity> findById(Long id);
    ScriptEntity save(ScriptEntity script);
}