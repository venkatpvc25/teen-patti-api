package com.pvc.game.feature.game.dto;

import com.pvc.game.feature.game.entity.GameType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GameCatalogItem {
    private GameType type;
    private String name;
    private boolean enabled;
    private int minPlayers;
    private int maxPlayers;
}
