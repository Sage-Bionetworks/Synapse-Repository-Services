package org.sagebionetworks.repo.manager.oauth.claimprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;

import com.google.common.collect.ImmutableList;

@RunWith(MockitoJUnitRunner.class)
public class TeamClaimProviderTest {
	
	@Mock
	private GroupMembersDAO groupMembersDAO;
	
	@InjectMocks
	private TeamClaimProvider claimProvider;
	
	private static final String USER_ID = "101";
	private static final String TEAM_ID = "999";
	
	private OIDCClaimsRequestDetails teamRequest;
	
	@Before
	public void setUp() {
		teamRequest = new OIDCClaimsRequestDetails();
		teamRequest.setValues(ImmutableList.of(TEAM_ID, "102"));
	}

	@Test
	public void testClaim() {
		List<String> teams = Collections.singletonList(TEAM_ID); // the list of teams to which the user belongs
		when(groupMembersDAO.filterUserGroups(USER_ID, ImmutableList.of("102",TEAM_ID))).thenReturn(teams);
		// method under test
		assertEquals(OIDCClaimName.team, claimProvider.getName());
		// method under test
		assertNotNull(claimProvider.getDescription());
		
		// method under test
		assertEquals(Collections.singletonList(TEAM_ID), claimProvider.getClaim(USER_ID, teamRequest));
	}

	@Test
	public void testClaimEmpty() {
		// what if the user belongs to no teams?
		when(groupMembersDAO.filterUserGroups(USER_ID, ImmutableList.of("102",TEAM_ID))).thenReturn(Collections.EMPTY_LIST);
		// method under test
		assertEquals(Collections.EMPTY_LIST, claimProvider.getClaim(USER_ID, teamRequest));
	}

}
