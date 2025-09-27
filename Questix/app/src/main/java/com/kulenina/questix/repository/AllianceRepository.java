package com.kulenina.questix.repository;

import com.kulenina.questix.model.Alliance;

public class AllianceRepository extends Repository<Alliance> {
    public AllianceRepository() {
        super("alliances", Alliance.class);
    }
}
