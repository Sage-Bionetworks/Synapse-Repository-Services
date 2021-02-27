package org.sagebionetworks.repo.manager.team;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
public class TeamFileHandleAssociationProviderTest {

	@Mock
	private TeamDAO mockTeamDAO;
	
	@Mock
	private Team mockTeam;
	
	@Mock
	private JdbcTemplate mockJdbcTemplate;
	
	@Mock
	private NamedParameterJdbcTemplate mockNamedJdbcTemplate;
	
	@InjectMocks
	private TeamFileHandleAssociationProvider provider;

	String teamId = "1";
	String fileHandleId = "2";
	
	@Test
	public void testGetFileHandleIdsAssociatedWithObject() {
		when(mockTeamDAO.get(teamId)).thenReturn(mockTeam);
		when(mockTeam.getIcon()).thenReturn(fileHandleId);
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(
				Arrays.asList(fileHandleId, "4"), teamId);
		assertEquals(Collections.singleton(fileHandleId), associated);
	}

	@Test
	public void testGetObjectTypeForAssociatedType() {
		assertEquals(ObjectType.TEAM, provider.getAuthorizationObjectTypeForAssociatedObjectType());
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithObject_teamNotFoundException(){
		when(mockTeamDAO.get(teamId)).thenThrow(NotFoundException.class);
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(
				Arrays.asList(fileHandleId, "4"), teamId);
		assertEquals(Collections.emptySet(), associated);
	}

}
