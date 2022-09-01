package org.sagebionetworks.repo.model;

public enum TeamState {
    // A team is open when a user can create a membership request for the team
    OPEN,
    // A team is public when a user can join without creating a membership request
    PUBLIC,
    // A team is closed when a user cannot create a membership request
    CLOSED;

    public static TeamState from(Team team) {
        boolean canPublicJoin = Boolean.TRUE.equals(team.getCanPublicJoin());
        boolean canRequestMembership = !Boolean.FALSE.equals(team.getCanRequestMembership());

        if (canPublicJoin && canRequestMembership) {
            throw new IllegalArgumentException("It is a conflict to set both canPublicJoin and canRequestMembership to true.");
        }

        if (canPublicJoin) {
            return PUBLIC;
        }

        if (canRequestMembership) {
            return OPEN;
        }

        return CLOSED;

    }
}
