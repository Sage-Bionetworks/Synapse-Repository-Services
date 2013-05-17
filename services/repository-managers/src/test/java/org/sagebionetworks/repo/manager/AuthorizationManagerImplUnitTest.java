package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.manager.EvaluationManager;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.NodeInheritanceDAO;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;

public class AuthorizationManagerImplUnitTest {
	
	NodeInheritanceDAO mockNodeInheritanceDAO;	
	AccessControlListDAO mockAccessControlListDAO;	
	AccessRequirementDAO  mockAccessRequirementDAO;
	EvaluationManager mockEvaluationManager;
	ActivityDAO mockActivityDAO;
	NodeQueryDao mockNodeQueryDao;	
	NodeDAO mockNodeDAO;
	UserManager mockUserManager;
	FileHandleDao mockFileHandleDao;
	EvaluationDAO mockEvaluationDAO;
	
	private static String COMP_ID = "123";
	
	AuthorizationManager authorizationManager = null;
	UserInfo userInfo = null;
	UserInfo adminUser = null;
	
	Evaluation evaluation = null;
	
	@Before
	public void setUp() throws Exception {
		mockNodeInheritanceDAO = mock(NodeInheritanceDAO.class);	
		mockAccessControlListDAO = mock(AccessControlListDAO.class);	
		mockAccessRequirementDAO = mock(AccessRequirementDAO.class);
		mockActivityDAO = mock(ActivityDAO.class);
		mockNodeQueryDao = mock(NodeQueryDao.class);	
		mockNodeDAO = mock(NodeDAO.class);
		mockUserManager = mock(UserManager.class);
		mockEvaluationManager = mock(EvaluationManager.class);
		mockFileHandleDao = mock(FileHandleDao.class);
		mockEvaluationDAO = mock(EvaluationDAO.class);
		
		authorizationManager = new AuthorizationManagerImpl(
				mockNodeInheritanceDAO, mockAccessControlListDAO,
				mockAccessRequirementDAO, mockActivityDAO, mockNodeQueryDao,
				mockNodeDAO, mockUserManager, mockEvaluationManager, mockFileHandleDao,
				mockEvaluationDAO);
		
		userInfo = new UserInfo(false);
		UserGroup userInfoGroup = new UserGroup();
		userInfoGroup.setId("123");
		userInfo.setIndividualGroup(userInfoGroup);
		userInfo.setUser(new User());
		userInfo.setGroups(new ArrayList<UserGroup>());
		adminUser = new UserInfo(true);
		UserGroup adminInfoGroup = new UserGroup();
		adminInfoGroup.setId("456");
		adminUser.setIndividualGroup(adminInfoGroup);	
		
		Node mockNode = mock(Node.class);
		when(mockNode.getCreatedByPrincipalId()).thenReturn(-1l);
		when(mockNodeDAO.getNode(any(String.class))).thenReturn(mockNode);
		
		evaluation = new Evaluation();
		evaluation.setId(COMP_ID);
		evaluation.setOwnerId("987");
		when(mockEvaluationDAO.get(COMP_ID)).thenReturn(evaluation);

		when(mockAccessRequirementDAO.unmetAccessRequirements(
				any(RestrictableObjectDescriptor.class), any(Collection.class), eq(ACCESS_TYPE.PARTICIPATE))).
				thenReturn(new ArrayList<Long>());
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
		PaginatedResults<Reference> results1 = generateQueryResults(limit, total);
		PaginatedResults<Reference> results2 = generateQueryResults(total-limit, total);		
		PaginatedResults<Reference> results3 = generateQueryResults(total-(2*limit), total);
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
		PaginatedResults<Reference> results1 = generateQueryResults(1, 1);		
		when(mockActivityDAO.getEntitiesGeneratedBy(actId, limit, offset)).thenReturn(results1);		
		
		boolean canAccess = authorizationManager.canAccessActivity(userInfo, actId);
		verify(mockActivityDAO).getEntitiesGeneratedBy(actId, limit, offset);
		assertFalse(canAccess);
	}
	
	@Test
	public void testCanAccessRawFileHandleByCreator(){
		// The admin can access anything
		String creator = userInfo.getIndividualGroup().getId();
		assertTrue("Admin should have access to all FileHandles",authorizationManager.canAccessRawFileHandleByCreator(adminUser, creator));
		assertTrue("Creator should have access to their own FileHandles", authorizationManager.canAccessRawFileHandleByCreator(userInfo, creator));
		// Set the creator to be the admin this time.
		creator = adminUser.getIndividualGroup().getId();
		assertFalse("Only the creator (or admin) should have access a FileHandle", authorizationManager.canAccessRawFileHandleByCreator(userInfo, creator));
	}
	
	@Test
	public void testCanAccessRawFileHandleById() throws NotFoundException{
		// The admin can access anything
		String creator = userInfo.getIndividualGroup().getId();
		String fileHandlId = "3333";
		when(mockFileHandleDao.getHandleCreator(fileHandlId)).thenReturn(creator);
		assertTrue("Admin should have access to all FileHandles",authorizationManager.canAccessRawFileHandleById(adminUser, fileHandlId));
		assertTrue("Creator should have access to their own FileHandles", authorizationManager.canAccessRawFileHandleById(userInfo, fileHandlId));
		// change the users id
		UserInfo notTheCreatoro = new UserInfo(false);
		UserGroup userInfoGroup = new UserGroup();
		userInfoGroup.setId("999999");
		notTheCreatoro.setIndividualGroup(userInfoGroup);
		assertFalse("Only the creator (or admin) should have access a FileHandle", authorizationManager.canAccessRawFileHandleById(notTheCreatoro, fileHandlId));
	}
	
	@Test
	public void testCanAccessWithObjectTypeAdmin() throws DatastoreException, NotFoundException{
		// Admin can always access
		assertTrue("An admin can access any entity",authorizationManager.canAccess(adminUser, "syn123", ObjectType.ENTITY, ACCESS_TYPE.DELETE));
		assertTrue("An admin can access any competition",authorizationManager.canAccess(adminUser, "334", ObjectType.EVALUATION, ACCESS_TYPE.DELETE));
	}
	
	@Test
	public void testCanAccessWithObjectTypeEntityAllow() throws DatastoreException, NotFoundException{
		String entityId = "syn123";
		// Setup to allow.
		when(mockAccessControlListDAO.canAccess(any(Collection.class), any(String.class), any(ACCESS_TYPE.class))).thenReturn(true);
		assertTrue("User should have acces to do anything with this entity", authorizationManager.canAccess(userInfo, entityId, ObjectType.ENTITY, ACCESS_TYPE.DELETE));
	}
	@Test
	public void testCanAccessWithObjectTypeEntityDeny() throws DatastoreException, NotFoundException{
		String entityId = "syn123";
		// Setup to deny.
		when(mockAccessControlListDAO.canAccess(any(Collection.class), any(String.class), any(ACCESS_TYPE.class))).thenReturn(false);
		assertFalse("User should not have acces to do anything with this entity", authorizationManager.canAccess(userInfo, entityId, ObjectType.ENTITY, ACCESS_TYPE.DELETE));
	}
	
	@Test
	public void testCanAccessWithObjectTypeCompetitionNonCompAdmin() throws DatastoreException, UnauthorizedException, NotFoundException{
		String compId = "123";
		// This user is not an admin but the should be able to read.
		when(mockEvaluationManager.isEvalAdmin(any(UserInfo.class), any(String.class))).thenReturn(false);
		assertTrue("User should have read access to any competition.", authorizationManager.canAccess(userInfo, compId, ObjectType.EVALUATION, ACCESS_TYPE.READ));
		assertFalse("User should not have delete access to this competition.", authorizationManager.canAccess(userInfo, compId, ObjectType.EVALUATION, ACCESS_TYPE.DELETE));
	}
	
	@Ignore // TODO Finish this
	@Test
	public void testCanAccessWithObjectTypeCompetitionCompAdmin() throws DatastoreException, UnauthorizedException, NotFoundException{
		// This user is not an admin but the should be able to read.
		when(mockEvaluationManager.isEvalAdmin(any(UserInfo.class), any(String.class))).thenReturn(true);
		assertTrue("A competition admin should have read access", authorizationManager.canAccess(userInfo, COMP_ID, ObjectType.EVALUATION, ACCESS_TYPE.READ));
		assertTrue("A competition admin should have delete access", authorizationManager.canAccess(userInfo, COMP_ID, ObjectType.EVALUATION, ACCESS_TYPE.DELETE));
		assertTrue("A user should have PARTICIPATE access to a competition", authorizationManager.canAccess(userInfo, COMP_ID, ObjectType.EVALUATION, ACCESS_TYPE.PARTICIPATE));
		when(mockEvaluationManager.isEvalAdmin(any(UserInfo.class), any(String.class))).thenReturn(false);
		assertTrue("A user should have PARTICIPATE access to a competition", authorizationManager.canAccess(userInfo, COMP_ID, ObjectType.EVALUATION, ACCESS_TYPE.PARTICIPATE));
	}
	
	@Ignore // TODO Finish this
	@Test
	public void testCanAccessWithObjectTypeCompetitionUnmetAccessRequirement() throws Exception {
		when(mockAccessRequirementDAO.unmetAccessRequirements(
				any(RestrictableObjectDescriptor.class), any(Collection.class), eq(ACCESS_TYPE.PARTICIPATE))).
				thenReturn(Arrays.asList(new Long[]{101L}));
		when(mockEvaluationManager.isEvalAdmin(any(UserInfo.class), any(String.class))).thenReturn(true);
		assertTrue("comp admin should always have PARTICIPATE access", authorizationManager.canAccess(userInfo, COMP_ID, ObjectType.EVALUATION, ACCESS_TYPE.PARTICIPATE));
		when(mockEvaluationManager.isEvalAdmin(any(UserInfo.class), any(String.class))).thenReturn(false);
		assertFalse("non-admin shouldn't have PARTICIPATE access if there are unmet requirements", authorizationManager.canAccess(userInfo, COMP_ID, ObjectType.EVALUATION, ACCESS_TYPE.PARTICIPATE));
	}
	

	private PaginatedResults<Reference> generateQueryResults(int numResults, int total) {
		PaginatedResults<Reference> results = new PaginatedResults<Reference>();
		List<Reference> resultList = new ArrayList<Reference>();		
		for(int i=0; i<numResults; i++) {
			Reference ref = new Reference();
			ref.setTargetId("nodeId");
			resultList.add(ref);
		}
		results.setResults(resultList);
		results.setTotalNumberOfResults(total);
		return results;
	}
	
}
