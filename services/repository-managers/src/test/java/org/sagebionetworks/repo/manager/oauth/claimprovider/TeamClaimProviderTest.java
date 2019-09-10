package org.sagebionetworks.repo.manager.oauth.claimprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;

import com.google.common.collect.ImmutableList;

@RunWith(MockitoJUnitRunner.class)
public class TeamClaimProviderTest {
	
	@Mock
	private TeamDAO mockTeamDAO;
	
	@InjectMocks
	private TeamClaimProvider claimProvider;
	
	private static final String USER_ID = "101";
	private static final String TEAM_ID = "999";
	
	private OIDCClaimsRequestDetails teamRequest;
	
	private ListWrapper<TeamMember> listWrapper;
	
	@Before
	public void setUp() {
		teamRequest = new OIDCClaimsRequestDetails();
		teamRequest.setValues(ImmutableList.of(TEAM_ID, "102"));
		
		TeamMember tm = new TeamMember();
		tm.setTeamId(TEAM_ID);
		listWrapper = new ListWrapper<TeamMember>();
		listWrapper.setList(Collections.singletonList(tm));
		when(mockTeamDAO.listMembers(ImmutableList.of(102L,999L), ImmutableList.of(Long.parseLong(USER_ID)))).thenReturn(listWrapper);
	}

	@Test
	public void testClaim() {
		// method under test
		assertEquals(OIDCClaimName.team, claimProvider.getName());
		// method under test
		assertNotNull(claimProvider.getDescription());
		
		// method under test
		assertEquals(Collections.singletonList(TEAM_ID), claimProvider.getClaim(USER_ID, teamRequest));
	}

	@Test
	public void testClaimEmpty() {
		listWrapper.setList(Collections.EMPTY_LIST);
		// method under test
		assertEquals(Collections.EMPTY_LIST, claimProvider.getClaim(USER_ID, teamRequest));
	}

}
