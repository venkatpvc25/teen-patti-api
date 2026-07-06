
package com.pvc.game.feature.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.pvc.game.feature.auth.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    List<User> findByUsernameIn(List<String> usernames);

    Optional<User> findByPhone(String phone);
}
