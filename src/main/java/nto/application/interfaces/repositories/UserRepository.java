package nto.application.interfaces.repositories;

import nto.core.entities.UserEntity;

import java.util.Optional;

public interface UserRepository {
    Optional<UserEntity> findById(Long id);

    UserEntity save(UserEntity user);

    Optional<UserEntity> findByUsername(String username);
}