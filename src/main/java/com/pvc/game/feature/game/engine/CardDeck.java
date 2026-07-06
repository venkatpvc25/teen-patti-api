package com.pvc.game.feature.game.engine;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CardDeck {

    private static final List<String> SUITS = List.of("S", "H", "D", "C");
    private static final List<String> RANKS = List.of("A", "K", "Q", "J", "10", "9", "8", "7", "6", "5", "4", "3", "2");

    private CardDeck() {
    }

    public static List<String> shuffled() {
        List<String> cards = new ArrayList<>();
        for (String suit : SUITS) {
            for (String rank : RANKS) {
                cards.add(rank + suit);
            }
        }
        Collections.shuffle(cards, new SecureRandom());
        return cards;
    }
}
