package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.competition.manager.CompetitionManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

public class AuthorizationManagerImplUnitTest {
	
	NodeInheritanceDAO mockNodeInheritanceDAO;	
	AccessControlListDAO mockAccessControlListDAO;	
	AccessRequirementDAO  mockAccessRequirementDAO;
	CompetitionManager mockCompetitionManager;
	ActivityDAO mockActivityDAO;
	NodeQueryDao mockNodeQueryDao;	
	NodeDAO mockNodeDAO;
	UserManager mockUserManager;
	
	AuthorizationManager authorizationManager = null;
	UserInfo userInfo = null;
	UserInfo adminUser = null;
	
	@Before
	public void setUp() throws Exception {
		mockNodeInheritanceDAO = mock(NodeInheritanceDAO.class);	
		mockAccessControlListDAO = mock(AccessControlListDAO.class);	
		mockAccessRequirementDAO = mock(AccessRequirementDAO.class);
		mockActivityDAO = mock(ActivityDAO.class);
		mockNodeQueryDao = mock(NodeQueryDao.class);	
		mockNodeDAO = mock(NodeDAO.class);
		mockUserManager = mock(UserManager.class);
		mockCompetitionManager = mock(CompetitionManager.class);
		
		authorizationManager = new AuthorizationManagerImpl(
				mockNodeInheritanceDAO, mockAccessControlListDAO,
				mockAccessRequirementDAO, mockActivityDAO, mockNodeQueryDao,
				mockNodeDAO, mockUserManager, mockCompetitionManager);
		
		userInfo = new UserInfo(false);
		UserGroup userInfoGroup = new UserGroup();
		userInfoGroup.setId("123");
		userInfo.setIndividualGroup(userInfoGroup);
		adminUser = new UserInfo(true);
		UserGroup adminInfoGroup = new UserGroup();
		adminInfoGroup.setId("456");
		adminUser.setIndividualGroup(adminInfoGroup);	
		
		Node mockNode = mock(Node.class);
		when(mockNode.getCreatedByPrincipalId()).thenReturn(-1l);
		when(mockNodeDAO.getNode(any(String.class))).thenReturn(mockNode);
	}

	@Test
	public void testCanAccessActivityPagination() throws Exception {		 
		Activity act = new Activity();
		String actId = "1";
		int limit = 1000;
		int total = 2001;
		int offset = 0;
		// create as admin, try to access as user so fails access and tests pagination
		act.setId(actId);
		act.setCreatedBy(adminUser.getIndividualGroup().getId());
		when(mockActivityDAO.get(actId)).thenReturn(act);
		QueryResults<String> results1 = generateQueryResults(limit, total);
		QueryResults<String> results2 = generateQueryResults(total-limit, total);		
		QueryResults<String> results3 = generateQueryResults(total-(2*limit), total);
		when(mockActivityDAO.getEntitiesGeneratedBy(actId, limit, offset)).thenReturn(results1);
		when(mockActivityDAO.getEntitiesGeneratedBy(actId, limit, offset+limit)).thenReturn(results2);		
		when(mockActivityDAO.getEntitiesGeneratedBy(actId, limit, offset+(2*limit))).thenReturn(results3);
		
		boolean canAccess = authorizationManager.canAccessActivity(userInfo, actId);
		verify(mockActivityDAO).getEntitiesGeneratedBy(actId, limit, offset);
		verify(mockActivityDAO).getEntitiesGeneratedBy(actId, limit, offset+limit);
		verify(mockActivityDAO).getEntitiesGeneratedBy(actId, limit, offset+(2*limit));
		assertFalse(canAccess);
	}

	@Test
	public void testCanAccessActivityPaginationSmallResultSet() throws Exception {		 
		Activity act = new Activity();
		String actId = "1";
		int limit = 1000;
		int offset = 0;
		// create as admin, try to access as user so fails access and tests pagination
		act.setId(actId);
		act.setCreatedBy(adminUser.getIndividualGroup().getId());
		when(mockActivityDAO.get(actId)).thenReturn(act);
		QueryResults<String> results1 = generateQueryResults(1, 1);		
		when(mockActivityDAO.getEntitiesGeneratedBy(actId, limit, offset)).thenReturn(results1);		
		
		boolean canAccess = authorizationManager.canAccessActivity(userInfo, actId);
		verify(mockActivityDAO).getEntitiesGeneratedBy(actId, limit, offset);
		assertFalse(canAccess);
	}
	
	@Test
	public void testCanAccessRawFileHandle(){
		// The admin can access anything
		String creator = userInfo.getIndividualGroup().getId();
		assertTrue("Admin should have access to all FileHandles",authorizationManager.canAccessRawFileHandle(adminUser, creator));
		assertTrue("Creator should have access to their own FileHandles", authorizationManager.canAccessRawFileHandle(userInfo, creator));
		// Set the creator to be the admin this time.
		creator = adminUser.getIndividualGroup().getId();
		assertFalse("Only the creator (or admin) should have access a FileHandle", authorizationManager.canAccessRawFileHandle(userInfo, creator));
	}
	
	@Test
	public void testCanAccessWithObjecTypeAdmin() throws DatastoreException, NotFoundException{
		// Admin can always access
		assertTrue("An admin can access any entity",authorizationManager.canAccess(adminUser, "syn123", ObjectType.ENTITY, ACCESS_TYPE.DELETE));
		assertTrue("An admin can access any competition",authorizationManager.canAccess(adminUser, "334", ObjectType.COMPETITION, ACCESS_TYPE.DELETE));
	}
	
	@Test
	public void testCanAccessWithObjecTypeEntityAllow() throws DatastoreException, NotFoundException{
		String entityId = "syn123";
		// Setup to allow.
		when(mockAccessControlListDAO.canAccess(any(Collection.class), any(String.class), any(ACCESS_TYPE.class))).thenReturn(true);
		assertTrue("User should have acces to do anything with this entity", authorizationManager.canAccess(userInfo, entityId, ObjectType.ENTITY, ACCESS_TYPE.DELETE));
	}
	@Test
	public void testCanAccessWithObjecTypeEntityDeny() throws DatastoreException, NotFoundException{
		String entityId = "syn123";
		// Setup to deny.
		when(mockAccessControlListDAO.canAccess(any(Collection.class), any(String.class), any(ACCESS_TYPE.class))).thenReturn(false);
		assertFalse("User should not have acces to do anything with this entity", authorizationManager.canAccess(userInfo, entityId, ObjectType.ENTITY, ACCESS_TYPE.DELETE));
	}
	
	@Test
	public void testCanAccessWithObjecTypeCompetitionNonCompAdmin() throws DatastoreException, UnauthorizedException, NotFoundException{
		String compId = "123";
		// This user is not an admin but the should be able to read.
		when(mockCompetitionManager.isCompAdmin(any(String.class), any(String.class))).thenReturn(false);
		assertTrue("User should have read access to any competition.", authorizationManager.canAccess(userInfo, compId, ObjectType.COMPETITION, ACCESS_TYPE.READ));
		assertFalse("User should not have delete access to this competition.", authorizationManager.canAccess(userInfo, compId, ObjectType.COMPETITION, ACCESS_TYPE.DELETE));
	}
	
	@Test
	public void testCanAccessWithObjecTypeCompetitionCompAdmin() throws DatastoreException, UnauthorizedException, NotFoundException{
		String compId = "123";
		// This user is not an admin but the should be able to read.
		when(mockCompetitionManager.isCompAdmin(any(String.class), any(String.class))).thenReturn(true);
		assertTrue("A competition admin should have read access", authorizationManager.canAccess(userInfo, compId, ObjectType.COMPETITION, ACCESS_TYPE.READ));
		assertTrue("A competition admin should have delete access", authorizationManager.canAccess(userInfo, compId, ObjectType.COMPETITION, ACCESS_TYPE.DELETE));
	}
	

	private QueryResults<String> generateQueryResults(int numResults, int total) {
		QueryResults<String> results = new QueryResults<String>();
		List<String> resultList = new ArrayList<String>();		
		for(int i=0; i<numResults; i++) {
			resultList.add("nodeId");
		}
		results.setResults(resultList);
		results.setTotalNumberOfResults(total);
		return results;
	}
	
}
