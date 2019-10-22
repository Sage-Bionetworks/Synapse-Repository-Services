package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.ProjectHeaderList;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.ProjectListTypeDeprecated;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.sagebionetworks.repo.web.service.UserProfileService;


@ExtendWith(MockitoExtension.class)
public class UserProfileControllerUnitTest {
	
	@Mock
	public ServiceProvider serviceProvider;
	
	@Mock
	public UserProfileService userProfileService;
	
	@InjectMocks
	public UserProfileController userProfileController;
	
	private static final Long USER_ID = 101L;
	private static final ProjectListSortColumn SORT_COLUMN = ProjectListSortColumn.LAST_ACTIVITY;
	private static final SortDirection SORT_DIRECTION = SortDirection.ASC;
	private static final Long OFFSET = 100L;
	private static final Long LIMIT = 20L;
	private static final String NEXT_PAGE_TOKEN = (new NextPageToken(LIMIT, OFFSET)).toToken();
	private List<ProjectHeader> projectHeaders;
	private ProjectHeaderList projectHeaderList;

	private void setUp() {
		when(serviceProvider.getUserProfileService()).thenReturn(userProfileService);
		
		projectHeaderList = new ProjectHeaderList();
		projectHeaders = new ArrayList<ProjectHeader>();
		for (int i=0; i<LIMIT; i++) {
			projectHeaders.add(new ProjectHeader());
		}
		projectHeaderList.setResults(projectHeaders);
		projectHeaderList.setNextPageToken(NEXT_PAGE_TOKEN);
	}
	
	@Test
	public void testGetOwnProjectsDeprecated() {
		setUp();
		when(userProfileService.getProjects(
				USER_ID, 
				USER_ID,
				null, // no Team ID
				ProjectListType.ALL, 
				SORT_COLUMN, 
				SORT_DIRECTION, 
				NEXT_PAGE_TOKEN)).
		thenReturn(projectHeaderList);
		
		// method under test
		PaginatedResults<ProjectHeader> paginated = 
				userProfileController.getOwnProjectsDeprecated(
				ProjectListTypeDeprecated.MY_PROJECTS,
				USER_ID,
				SORT_COLUMN,
				SORT_DIRECTION,
				OFFSET,
				LIMIT
				);
						
		verify(userProfileService).getProjects(
				USER_ID, 
				USER_ID,
				null, // no Team ID
				ProjectListType.ALL, 
				SORT_COLUMN, 
				SORT_DIRECTION, 
				NEXT_PAGE_TOKEN);
		
		assertEquals(projectHeaders, paginated.getResults());
		assertEquals(OFFSET+LIMIT+1, paginated.getTotalNumberOfResults());
	}
	
	@Test
	public void testGetProjectsTeamDeprecated() {
		setUp();
		Long teamId = 999L;
		when(userProfileService.getProjects(
				USER_ID, 
				USER_ID,
				teamId,
				ProjectListType.TEAM, 
				SORT_COLUMN, 
				SORT_DIRECTION, 
				NEXT_PAGE_TOKEN)).
		thenReturn(projectHeaderList);
		
		// method under test
		PaginatedResults<ProjectHeader> paginated = 
				userProfileController.getProjectsTeamDeprecated(
				teamId,
				USER_ID,
				SORT_COLUMN,
				SORT_DIRECTION,
				OFFSET,
				LIMIT
				);
						
		verify(userProfileService).getProjects(
				USER_ID, 
				USER_ID,
				teamId,
				ProjectListType.TEAM, 
				SORT_COLUMN, 
				SORT_DIRECTION, 
				NEXT_PAGE_TOKEN);
		
		assertEquals(projectHeaders, paginated.getResults());
		assertEquals(OFFSET+LIMIT+1, paginated.getTotalNumberOfResults());
	}
	
	@Test
	public void testGetProjectsUserDeprecated() {
		setUp();
		Long otherUserId = 202L;
		when(userProfileService.getProjects(
				USER_ID, 
				otherUserId,
				null, // no Team ID
				ProjectListType.ALL, 
				SORT_COLUMN, 
				SORT_DIRECTION, 
				NEXT_PAGE_TOKEN)).
		thenReturn(projectHeaderList);
		
		// method under test
		PaginatedResults<ProjectHeader> paginated = 
				userProfileController.getProjectsUserDeprecated(
				ProjectListTypeDeprecated.MY_PROJECTS,
				otherUserId,
				USER_ID,
				SORT_COLUMN,
				SORT_DIRECTION,
				OFFSET,
				LIMIT
				);
						
		verify(userProfileService).getProjects(
				USER_ID,
				otherUserId, 
				null, // no Team ID
				ProjectListType.ALL, 
				SORT_COLUMN, 
				SORT_DIRECTION, 
				NEXT_PAGE_TOKEN);
		
		assertEquals(projectHeaders, paginated.getResults());
		assertEquals(OFFSET+LIMIT+1, paginated.getTotalNumberOfResults());
	}
	
	
	@Test
	public void testgetProjectListTypeForProjectListType() {
		assertEquals(ProjectListType.CREATED, UserProfileController.
				getProjectListTypeForProjectListType(ProjectListTypeDeprecated.MY_CREATED_PROJECTS));
		assertEquals(ProjectListType.PARTICIPATED, UserProfileController.
				getProjectListTypeForProjectListType(ProjectListTypeDeprecated.MY_PARTICIPATED_PROJECTS));
		assertEquals(ProjectListType.ALL, UserProfileController.
				getProjectListTypeForProjectListType(ProjectListTypeDeprecated.MY_PROJECTS));
		assertEquals(ProjectListType.TEAM, UserProfileController.
				getProjectListTypeForProjectListType(ProjectListTypeDeprecated.MY_TEAM_PROJECTS));
		assertEquals(ProjectListType.ALL, UserProfileController.
				getProjectListTypeForProjectListType(ProjectListTypeDeprecated.OTHER_USER_PROJECTS));
		assertEquals(ProjectListType.TEAM, UserProfileController.
				getProjectListTypeForProjectListType(ProjectListTypeDeprecated.TEAM_PROJECTS));
	}

}
