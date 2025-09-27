package com.kulenina.questix.repository;

import com.kulenina.questix.model.AllianceInvitation;

public class AllianceInvitationRepository extends Repository<AllianceInvitation> {
    public AllianceInvitationRepository() {
        super("alliance_invitations", AllianceInvitation.class);
    }
}
