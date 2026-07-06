package com.pvc.game.feature.game.entity;

import java.time.Instant;
import java.util.UUID;

import com.pvc.game.feature.auth.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "match_players")
@Getter
@Setter
public class MatchPlayer {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int seatNo;

    @Column(nullable = false)
    private long chipsCommitted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayerResult result = PlayerResult.PLAYING;

    @Column(nullable = false)
    private Instant joinedAt = Instant.now();
}
