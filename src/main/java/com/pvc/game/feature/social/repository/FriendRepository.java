package com.pvc.game.feature.social.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pvc.game.feature.auth.entity.User;
import com.pvc.game.feature.social.entity.Friend;

public interface FriendRepository extends JpaRepository<Friend, UUID> {
    List<Friend> findByUser(User user);
    boolean existsByUserAndFriendUser(User user, User friendUser);
}
