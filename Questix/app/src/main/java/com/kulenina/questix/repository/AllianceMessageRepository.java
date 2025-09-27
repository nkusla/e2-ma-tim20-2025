package com.kulenina.questix.repository;

import com.kulenina.questix.model.AllianceMessage;

public class AllianceMessageRepository extends Repository<AllianceMessage> {
    public AllianceMessageRepository() {
        super("alliance_messages", AllianceMessage.class);
    }
}
