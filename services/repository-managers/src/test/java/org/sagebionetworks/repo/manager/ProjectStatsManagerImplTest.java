package org.sagebionetworks.repo.manager;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ProjectStat;
import org.sagebionetworks.repo.model.ProjectStatsDAO;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.WikiPageKeyHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProjectStatsManagerImplTest {

	@Mock
	ProjectStatsDAO mockProjectStatDao;
	
	@Mock
	NodeDAO mockNodeDao;
	
	@Mock
	V2WikiPageDao mockV2wikiPageDao;
	
	@Mock
	AuthorizationManager mockAuthorizationManager;
	
	@Mock
	UserGroupDAO mockUserGroupDao;
	
	@Mock
	GroupMembersDAO mockGroupMemberDao;
	
	ProjectStatsManagerImpl manager;
	
	Long projectId;
	String projectIdString;
	String entityId;
	String wikiId;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		manager = new ProjectStatsManagerImpl();
		ReflectionTestUtils.setField(manager, "projectStatDao", mockProjectStatDao);
		ReflectionTestUtils.setField(manager, "nodeDao", mockNodeDao);
		ReflectionTestUtils.setField(manager, "v2wikiPageDao", mockV2wikiPageDao);
		ReflectionTestUtils.setField(manager, "authorizationManager", mockAuthorizationManager);
		ReflectionTestUtils.setField(manager, "userGroupDao", mockUserGroupDao);
		ReflectionTestUtils.setField(manager, "groupMemberDao", mockGroupMemberDao);
		
		projectId = 456L;
		projectIdString = KeyFactory.keyToString(projectId);
		entityId = "syn123";
		when(mockNodeDao.getProjectId(entityId)).thenReturn(Optional.of(projectIdString));
		
		wikiId = "999";
		when(mockV2wikiPageDao.lookupWikiKey(wikiId)).thenReturn(WikiPageKeyHelper.createWikiPageKey(entityId,ObjectType.ENTITY , wikiId));
	}
	
	@Test
	public void testGetProjectForObjectEntity(){
		ObjectType type = ObjectType.ENTITY;
		// call under test
		String projectIdLookup = manager.getProjectForObject(entityId, type).orElseThrow();
		assertEquals(projectIdString, projectIdLookup);
	}
	
	@Test
	public void testGetProjectForObjectTable(){
		ObjectType type = ObjectType.TABLE;
		// call under test
		String projectIdLookup = manager.getProjectForObject(entityId, type).orElseThrow();
		assertEquals(projectIdString, projectIdLookup);
	}
	
	@Test
	public void testGetProjectForObjectWiki(){
		ObjectType type = ObjectType.WIKI;
		// call under test
		String projectIdLookup = manager.getProjectForObject(wikiId, type).orElseThrow();
		assertEquals(projectIdString, projectIdLookup);
	}
	
	@Test
	public void testGetProjectForObjectUnknown(){
		// a favorite does not have a project
		ObjectType type = ObjectType.FAVORITE;
		// call under test
		Optional<String> projectIdLookup = manager.getProjectForObject(wikiId, type);
		assertEquals("Favorites do not have projects so null should be returned.",Optional.empty(), projectIdLookup);
	}
	
	@Test
	public void testGetProjectForObjectNotFound(){
		// setup a not found case
		when(mockNodeDao.getProjectId(entityId)).thenReturn(Optional.empty());
		ObjectType type = ObjectType.ENTITY;
		// call under test
		Optional<String> projectIdLookup = manager.getProjectForObject(entityId, type);
		assertEquals("Null should be returned when the object cannot be found.",Optional.empty(), projectIdLookup);
	}
	
	@Test
	public void testUpdateProjectStatsUser(){
		Long userId = 707L;
		when(mockUserGroupDao.isIndividual(userId)).thenReturn(true);
		ObjectType type = ObjectType.ENTITY;
		Date activityDate = new Date(1);
		// call under test
		manager.updateProjectStats(userId, entityId, type, activityDate);
		verify(mockProjectStatDao).updateProjectStat(new ProjectStat(projectId, userId, activityDate));
		verify(mockGroupMemberDao, never()).getMemberIds(anyLong());
	}
	
	@Test
	public void testUpdateProjectStatsTeam(){
		Long principalId = 707L;
		when(mockUserGroupDao.isIndividual(principalId)).thenReturn(false);
		Long memberIdOne = 111L;
		Long memberIdTwo = 222L;
		when(mockGroupMemberDao.getMemberIds(principalId)).thenReturn(Sets.newLinkedHashSet(Arrays.asList(memberIdOne, memberIdTwo)));

		ObjectType type = ObjectType.ENTITY;
		Date activityDate = new Date(1);
		// call under test
		manager.updateProjectStats(principalId, entityId, type, activityDate);
		// stats should be updated for each member
		ProjectStat[] batchUpdate = new ProjectStat[]{
				new ProjectStat(projectId, memberIdOne, activityDate),
				new ProjectStat(projectId, memberIdTwo, activityDate)
		};
		verify(mockProjectStatDao).updateProjectStat(batchUpdate);
	}
	
	@Test
	public void testUpdateProjectStatsTeamNotMembers(){
		Long principalId = 707L;
		when(mockUserGroupDao.isIndividual(principalId)).thenReturn(false);
		when(mockGroupMemberDao.getMemberIds(principalId)).thenReturn(new HashSet<Long>());
		
		ObjectType type = ObjectType.ENTITY;
		Date activityDate = new Date(1);
		// call under test
		manager.updateProjectStats(principalId, entityId, type, activityDate);
		// should not be called
		verify(mockProjectStatDao, never()).updateProjectStat(any());
	}
	
	@Test
	public void testUpdateProjectStatsNotFound(){
		// setup a not found case
		when(mockNodeDao.getProjectId(entityId)).thenReturn(Optional.empty());
		Long userId = 707L;
		ObjectType type = ObjectType.ENTITY;
		Date activityDate = new Date(1);
		// call under test
		manager.updateProjectStats(userId, entityId, type, activityDate);
		
		verify(mockProjectStatDao, never()).updateProjectStat(any(ProjectStat.class));
	}
	
	@Test
	public void testMemberAddedToTeam() {
		Long teamId = 99L;
		Long memberId = 888L;
		Date activityDate = new Date();
		Long projectId1 = 111L;
		Long projectId2 = 222L;
		Set<Long> visibleProjectIds = Sets.newLinkedHashSet(Arrays.asList(projectId1, projectId2));
		// the projects visible to the team
		when(mockAuthorizationManager.getAccessibleProjectIds(Sets
						.newLinkedHashSet(Arrays.asList(teamId)))).thenReturn(visibleProjectIds);
		
		// call under test
		manager.memberAddedToTeam(teamId, memberId, activityDate);
		// batch update
		ProjectStat[] batchUpdate = new ProjectStat[]{
				new ProjectStat(projectId1, memberId, activityDate),
				new ProjectStat(projectId2, memberId, activityDate)
		};
		verify(mockProjectStatDao).updateProjectStat(batchUpdate);
	}
	
	@Test
	public void testMemberAddedToTeamWithNoProjects(){
		Long teamId = 99L;
		Long memberId = 888L;
		Date activityDate = new Date();
		Set<Long> empty = new HashSet<Long>();
		// the projects visible to the team
		when(mockAuthorizationManager.getAccessibleProjectIds(Sets
						.newLinkedHashSet(Arrays.asList(teamId)))).thenReturn(empty);
		
		// call under test
		manager.memberAddedToTeam(teamId, memberId, activityDate);
		// should not be called
		verify(mockProjectStatDao, never()).updateProjectStat(any());
	}
}
