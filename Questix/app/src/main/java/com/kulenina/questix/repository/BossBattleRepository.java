package com.kulenina.questix.repository;

import com.kulenina.questix.model.BossBattle;

public class BossBattleRepository extends Repository<BossBattle> {
    public BossBattleRepository() {
        super("boss_battles", BossBattle.class);
    }
}
