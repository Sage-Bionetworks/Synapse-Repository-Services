package org.sagebionetworks.repo.model;

public enum TeamState {
    // A team is open when a user can create a membership request for the team
    OPEN(false, true),
    // A team is public when a user can join without creating a membership request
    PUBLIC(true, false),
    // A team is closed when a user cannot create a membership request
    CLOSED(false, false);

    private TeamState(boolean canPublicJoin, boolean canRequestMembership){
        this.canPublicJoin= canPublicJoin;
        this.canRequestMembership = canRequestMembership;
    }

    private boolean canPublicJoin;
    private boolean canRequestMembership;

    public boolean isCanPublicJoin() {
        return canPublicJoin;
    }

    public boolean isCanRequestMembership() {
        return canRequestMembership;
    }

    public static TeamState from(Team team) {
        boolean canPublicJoin = Boolean.TRUE.equals(team.getCanPublicJoin());
        boolean canRequestMembership;

        // The purpose of checking canPublicJoin is to maintain backward compatibility with existing clients that do not
        // provide a value for canRequestMembership so that a default value for canRequestMembership can be set without causing conflict.
        if (canPublicJoin || team.getCanRequestMembership() != null ) {
            canRequestMembership = Boolean.TRUE.equals(team.getCanRequestMembership());
        } else {
            canRequestMembership = true;
        }

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
