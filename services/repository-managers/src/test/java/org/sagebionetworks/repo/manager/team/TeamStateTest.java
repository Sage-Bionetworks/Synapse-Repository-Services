package org.sagebionetworks.repo.manager.team;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamState;


public class TeamStateTest {

    @Test
    public void testFromOpenToPublicAndOpenToMembershipRequests() {
        Team team = new Team();
        team.setCanPublicJoin(true);
        team.setCanRequestMembership(true);

        String errorMessage = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            TeamState.from(team);
        }).getMessage();

        Assertions.assertEquals("It is a conflict to set both canPublicJoin and canRequestMembership to true.", errorMessage);
    }

    @Test
    public void testFromOpenToPublicAndClosedToMembershipRequests() {
        Team team = new Team();
        team.setCanPublicJoin(true);
        team.setCanRequestMembership(false);

        // Call under test
        Assertions.assertEquals(TeamState.PUBLIC, TeamState.from(team));
    }


    @Test
    public void testFromOpenToPublicAndNullOpenToMembershipRequests() {
        Team team = new Team();
        team.setCanPublicJoin(true);
        team.setCanRequestMembership(null);

        String errorMessage = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            // Call under test
            TeamState.from(team);
        }).getMessage();

        Assertions.assertEquals("It is a conflict to set both canPublicJoin and canRequestMembership to true.", errorMessage);
    }

    @Test
    public void testFromClosedToPublicAndOpenToMembershipRequests() {
        Team team = new Team();
        team.setCanPublicJoin(false);
        team.setCanRequestMembership(true);

        // Call under test
        Assertions.assertEquals(TeamState.OPEN, TeamState.from(team));
    }

    @Test
    public void testFromClosedToPublicAndClosedToMembershipRequests() {
        Team team = new Team();
        team.setCanPublicJoin(false);
        team.setCanRequestMembership(false);

        // Call under test
        Assertions.assertEquals(TeamState.CLOSED, TeamState.from(team));
    }

    @Test
    public void testFromClosedToPublicAndNullOpenToMembershipRequests() {
        Team team = new Team();
        team.setCanPublicJoin(false);
        team.setCanRequestMembership(null);

        // Call under test
        Assertions.assertEquals(TeamState.OPEN, TeamState.from(team));
    }

    @Test
    public void testFromNullOpenToPublicAndOpenToMembershipRequests() {
        Team team = new Team();
        team.setCanPublicJoin(null);
        team.setCanRequestMembership(true);

        // Call under test
        Assertions.assertEquals(TeamState.OPEN, TeamState.from(team));
    }

    @Test
    public void testFromNullOpenToPublicAndClosedToMembershipRequests() {
        Team team = new Team();
        team.setCanPublicJoin(null);
        team.setCanRequestMembership(false);

        // Call under test
        Assertions.assertEquals(TeamState.CLOSED, TeamState.from(team));
    }

    @Test
    public void testFromNullOpenToPublicAndNullOpenToMembershipRequests() {
        Team team = new Team();
        team.setCanPublicJoin(null);
        team.setCanRequestMembership(null);

        // Call under test
        Assertions.assertEquals(TeamState.OPEN, TeamState.from(team));
    }
}

