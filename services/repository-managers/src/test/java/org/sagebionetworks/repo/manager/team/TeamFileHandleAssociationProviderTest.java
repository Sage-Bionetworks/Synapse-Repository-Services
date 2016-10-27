package org.sagebionetworks.repo.manager.team;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.springframework.test.util.ReflectionTestUtils;

public class TeamFileHandleAssociationProviderTest {

	@Mock
	private TeamDAO mockTeamDAO;
	@Mock
	private Team mockTeam;
	private TeamFileHandleAssociationProvider provider;
	
	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		provider = new TeamFileHandleAssociationProvider();
		ReflectionTestUtils.setField(provider, "teamDAO", mockTeamDAO);
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithObject() {
		String teamId = "1";
		String fileHandleId = "2";
		when(mockTeamDAO.get(teamId)).thenReturn(mockTeam);
		when(mockTeam.getIcon()).thenReturn(fileHandleId);
		Set<String> associated = provider.getFileHandleIdsAssociatedWithObject(
				Arrays.asList(fileHandleId, "4"), teamId);
		assertEquals(Collections.singleton(fileHandleId), associated);
	}

	@Test
	public void testGetObjectTypeForAssociatedType() {
		assertEquals(ObjectType.TEAM, provider.getAuthorizationObjectTypeForAssociatedObjectType());
	}
}
