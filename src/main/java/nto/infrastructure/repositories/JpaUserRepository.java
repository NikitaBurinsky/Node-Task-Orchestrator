package nto.infrastructure.repositories;

import nto.application.interfaces.repositories.UserRepository;
import nto.core.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaUserRepository extends JpaRepository<UserEntity, Long>, UserRepository {

    // Дополнительный метод Spring Data (не входит в базовый интерфейс, но полезен)
    Optional<UserEntity> findByUsername(String username);
}