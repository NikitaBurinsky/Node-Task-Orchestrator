package nto.infrastructure.repositories;

import nto.core.entities.SshUsernameEntity;
import nto.core.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaSshUsernameRepository extends JpaRepository<SshUsernameEntity, Long> {

    Optional<SshUsernameEntity> findByOwnerAndUsername(UserEntity owner, String username);
}
