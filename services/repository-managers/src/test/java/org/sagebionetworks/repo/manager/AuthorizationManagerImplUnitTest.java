package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_AND_COMPLIANCE_TEAM_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.manager.EvaluationManager;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
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
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;

public class AuthorizationManagerImplUnitTest {
	
	NodeInheritanceDAO mockNodeInheritanceDAO;	
	AccessControlListDAO mockAccessControlListDAO;	
	AccessRequirementDAO  mockAccessRequirementDAO;
	AccessApprovalDAO mockAccessApprovalDAO;
	EvaluationManager mockEvaluationManager;
	ActivityDAO mockActivityDAO;
	NodeQueryDao mockNodeQueryDao;	
	NodeDAO mockNodeDAO;
	UserManager mockUserManager;
	FileHandleDao mockFileHandleDao;
	EvaluationDAO mockEvaluationDAO;
	
	
	private static String USER_PRINCIPAL_ID = "123";
	private static String EVAL_OWNER_PRINCIPAL_ID = "987";
	private static String EVAL_ID = "1234567";
	
	private AuthorizationManagerImpl authorizationManager = null;
	private UserInfo userInfo = null;
	private UserInfo adminUser = null;
	
	private Evaluation evaluation = null;
	
	private UserGroup actTeam = null;
	private UserGroupDAO mockUserGroupDAO = null;

	@Before
	public void setUp() throws Exception {
		mockNodeInheritanceDAO = mock(NodeInheritanceDAO.class);	
		mockAccessControlListDAO = mock(AccessControlListDAO.class);	
		mockAccessRequirementDAO = mock(AccessRequirementDAO.class);
		mockAccessApprovalDAO = mock(AccessApprovalDAO.class);
		mockActivityDAO = mock(ActivityDAO.class);
		mockNodeQueryDao = mock(NodeQueryDao.class);	
		mockNodeDAO = mock(NodeDAO.class);
		mockUserManager = mock(UserManager.class);
		mockEvaluationManager = mock(EvaluationManager.class);
		mockFileHandleDao = mock(FileHandleDao.class);
		mockEvaluationDAO = mock(EvaluationDAO.class);
		mockUserGroupDAO = Mockito.mock(UserGroupDAO.class);
		
		authorizationManager = new AuthorizationManagerImpl(
				mockNodeInheritanceDAO, mockAccessControlListDAO,
				mockAccessRequirementDAO, mockAccessApprovalDAO, 
				mockActivityDAO, mockNodeQueryDao,
				mockNodeDAO, mockUserManager, mockFileHandleDao,
				mockEvaluationDAO, mockUserGroupDAO);
		
		actTeam = new UserGroup();
		actTeam.setId("101");
		actTeam.setIsIndividual(false);
		actTeam.setName(ACCESS_AND_COMPLIANCE_TEAM_NAME);
		when(mockUserGroupDAO.findGroup(ACCESS_AND_COMPLIANCE_TEAM_NAME, false)).thenReturn(actTeam);

		userInfo = new UserInfo(false);
		UserGroup userInfoGroup = new UserGroup();
		userInfoGroup.setId(USER_PRINCIPAL_ID);
		userInfo.setIndividualGroup(userInfoGroup);
		User user = new User();
		user.setId("not_anonymous");
		userInfo.setUser(user);
		userInfo.setGroups(new ArrayList<UserGroup>());
		adminUser = new UserInfo(true);
		UserGroup adminInfoGroup = new UserGroup();
		adminInfoGroup.setId("456");
		adminUser.setIndividualGroup(adminInfoGroup);	
		
		Node mockNode = mock(Node.class);
		when(mockNode.getCreatedByPrincipalId()).thenReturn(-1l);
		when(mockNodeDAO.getNode(any(String.class))).thenReturn(mockNode);
		
		evaluation = new Evaluation();
		evaluation.setId(EVAL_ID);
		evaluation.setOwnerId(EVAL_OWNER_PRINCIPAL_ID);
		when(mockEvaluationDAO.get(EVAL_ID)).thenReturn(evaluation);

		when(mockAccessRequirementDAO.unmetAccessRequirements(
				any(RestrictableObjectDescriptor.class), any(Collection.class), eq(ACCESS_TYPE.PARTICIPATE))).
				thenReturn(new ArrayList<Long>());
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
		// This user is not an admin but the should be able to read.
		assertTrue("User should have read access to any competition.", authorizationManager.canAccess(userInfo, EVAL_ID, ObjectType.EVALUATION, ACCESS_TYPE.READ));
		assertFalse("User should not have delete access to this competition.", authorizationManager.canAccess(userInfo, EVAL_ID, ObjectType.EVALUATION, ACCESS_TYPE.DELETE));
		assertTrue("A user should have PARTICIPATE access to an evaluation", authorizationManager.canAccess(userInfo, EVAL_ID, ObjectType.EVALUATION, ACCESS_TYPE.PARTICIPATE));
	}
	
	@Test
	public void testCanAccessWithObjectTypeCompetitionCompAdmin() throws DatastoreException, UnauthorizedException, NotFoundException{
		// make the user the creator of the evaluation
		userInfo.getIndividualGroup().setId(EVAL_OWNER_PRINCIPAL_ID);
		assertTrue("An evaluation admin should have read access", authorizationManager.canAccess(userInfo, EVAL_ID, ObjectType.EVALUATION, ACCESS_TYPE.READ));
		assertTrue("An evaluation admin should have delete access", authorizationManager.canAccess(userInfo, EVAL_ID, ObjectType.EVALUATION, ACCESS_TYPE.DELETE));
		assertTrue("An evaluation admin should have PARTICIPATE access to an evaluation", authorizationManager.canAccess(userInfo, EVAL_ID, ObjectType.EVALUATION, ACCESS_TYPE.PARTICIPATE));
	}
	
	@Test
	public void testCanAccessWithObjectTypeCompetitionUnmetAccessRequirement() throws Exception {
		when(mockAccessRequirementDAO.unmetAccessRequirements(
				any(RestrictableObjectDescriptor.class), any(Collection.class), eq(ACCESS_TYPE.PARTICIPATE))).
				thenReturn(Arrays.asList(new Long[]{101L}));
		// takes a little more to mock admin access to evaluation
		String origEvaluationOwner = evaluation.getOwnerId();
		evaluation.setOwnerId(userInfo.getIndividualGroup().getId());
		assertFalse("admin shouldn't have PARTICIPATE access if there are unmet requirements", authorizationManager.canAccess(userInfo, EVAL_ID, ObjectType.EVALUATION, ACCESS_TYPE.PARTICIPATE));
		evaluation.setOwnerId(origEvaluationOwner);
		assertFalse("non-admin shouldn't have PARTICIPATE access if there are unmet requirements", authorizationManager.canAccess(userInfo, EVAL_ID, ObjectType.EVALUATION, ACCESS_TYPE.PARTICIPATE));
	}
	

	@Test
	public void testVerifyACTTeamMembershipOrIsAdmin_Admin() {
		UserInfo adminInfo = new UserInfo(true);
		assertTrue(authorizationManager.isACTTeamMemberOrAdmin(adminInfo));
	}

	@Test
	public void testVerifyACTTeamMembershipOrIsAdmin_ACT() {
		userInfo.getGroups().add(actTeam);
		assertTrue(authorizationManager.isACTTeamMemberOrAdmin(userInfo));
	}

	@Test
	public void testVerifyACTTeamMembershipOrIsAdmin_NONE() {
		assertFalse(authorizationManager.isACTTeamMemberOrAdmin(userInfo));
	}

	@Test
	public void testVerifyACTTeamMembershipOrCanCreateOrEdit_isAdmin() throws Exception {
		List<String> ids = new ArrayList<String>(Arrays.asList(new String[]{"101"}));
		assertTrue(authorizationManager.isACTTeamMemberOrCanCreateOrEdit(adminUser, ids));
	}

	@Test
	public void testVerifyACTTeamMembershipOrCanCreateOrEdit_ACT() throws Exception {
		userInfo.getGroups().add(actTeam);
		List<String> ids = new ArrayList<String>(Arrays.asList(new String[]{"101"}));
		assertTrue(authorizationManager.isACTTeamMemberOrCanCreateOrEdit(userInfo, ids));
	}

	@Test
	public void testVerifyACTTeamMembershipOrCanCreateOrEdit_multiple() throws Exception {
		List<String> ids = new ArrayList<String>(Arrays.asList(new String[]{"101", "102"}));
		assertFalse(authorizationManager.isACTTeamMemberOrCanCreateOrEdit(userInfo, ids));
	}

	@Test
	public void testVerifyACTTeamMembershipOrCanCreateOrEdit_editAccess() throws Exception {
		List<String> ids = new ArrayList<String>(Arrays.asList(new String[]{"101"}));
		when(mockNodeInheritanceDAO.getBenefactor("101")).thenReturn("101");
		when(mockAccessControlListDAO.canAccess(eq(userInfo.getGroups()), eq("101"), any(ACCESS_TYPE.class))).thenReturn(true);
		assertTrue(authorizationManager.isACTTeamMemberOrCanCreateOrEdit(userInfo, ids));
	}

	@Test
	public void testVerifyACTTeamMembershipOrCanCreateOrEdit_none() throws Exception {
		List<String> ids = new ArrayList<String>(Arrays.asList(new String[]{"101"}));
		when(mockNodeInheritanceDAO.getBenefactor("101")).thenReturn("101");
		when(mockAccessControlListDAO.canAccess(eq(userInfo.getGroups()), eq("101"), any(ACCESS_TYPE.class))).thenReturn(false);
		assertFalse(authorizationManager.isACTTeamMemberOrCanCreateOrEdit(userInfo, ids));
	}
	
	private static RestrictableObjectDescriptor createEntitySubjectId() {
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setType(RestrictableObjectType.ENTITY);
		subjectId.setId("101");
		return subjectId;
	}
	
	private AccessRequirement createEntityAccessRequirement() throws Exception {
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{createEntitySubjectId()}));
		ar.setId(1234L);
		when(mockAccessRequirementDAO.get(ar.getId().toString())).thenReturn(ar);
		return ar;
	}
	
	private AccessApproval createEntityAccessApproval() throws Exception {
		AccessRequirement ar = createEntityAccessRequirement();
		TermsOfUseAccessApproval aa = new TermsOfUseAccessApproval();
		aa.setAccessorId(userInfo.getIndividualGroup().getId());
		aa.setId(656L);
		aa.setRequirementId(ar.getId());
		when(mockAccessApprovalDAO.get(aa.getId().toString())).thenReturn(aa);
		return aa;
	}

	private static RestrictableObjectDescriptor createEvaluationSubjectId() {
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setType(RestrictableObjectType.EVALUATION);
		subjectId.setId(EVAL_ID);
		return subjectId;
	}
	
	private AccessRequirement createEvaluationAccessRequirement() throws Exception {
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{createEvaluationSubjectId()}));
		ar.setId(1234L);
		when(mockAccessRequirementDAO.get(ar.getId().toString())).thenReturn(ar);
		return ar;
	}
	
	private AccessApproval createEvaluationAccessApproval() throws Exception {
		AccessRequirement ar = createEvaluationAccessRequirement();
		TermsOfUseAccessApproval aa = new TermsOfUseAccessApproval();
		aa.setAccessorId(userInfo.getIndividualGroup().getId());
		aa.setId(656L);
		aa.setRequirementId(ar.getId());
		when(mockAccessApprovalDAO.get(aa.getId().toString())).thenReturn(aa);
		return aa;
	}

	@Test
	public void testCanAccessEntityAccessRequirement() throws Exception {
		AccessRequirement ar = createEntityAccessRequirement();
		assertFalse(authorizationManager.canAccess(userInfo, ar.getId().toString(), ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.UPDATE));
		userInfo.getGroups().add(actTeam);
		assertTrue(authorizationManager.canAccess(userInfo, "1234", ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.UPDATE));
	}
	
	@Test
	public void testCanAccessEvaluationAccessRequirement() throws Exception {
		AccessRequirement ar = createEvaluationAccessRequirement();
		assertFalse(authorizationManager.canAccess(userInfo, ar.getId().toString(), ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.UPDATE));
		userInfo.getIndividualGroup().setId(EVAL_OWNER_PRINCIPAL_ID);
		assertTrue(authorizationManager.canAccess(userInfo, ar.getId().toString(), ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.UPDATE));
	}
	
	@Test
	public void testCanAccessEntityAccessApproval() throws Exception {
		AccessApproval aa = createEntityAccessApproval();
		assertFalse(authorizationManager.canAccess(userInfo, aa.getId().toString(), ObjectType.ACCESS_APPROVAL, ACCESS_TYPE.READ));
		userInfo.getGroups().add(actTeam);
		assertTrue(authorizationManager.canAccess(userInfo, aa.getId().toString(), ObjectType.ACCESS_APPROVAL, ACCESS_TYPE.READ));
	}
	
	@Test
	public void testCanAccessEvaluationAccessApproval() throws Exception {
		AccessApproval aa = createEvaluationAccessApproval();
		assertFalse(authorizationManager.canAccess(userInfo, aa.getId().toString(), ObjectType.ACCESS_APPROVAL, ACCESS_TYPE.READ));
		userInfo.getIndividualGroup().setId(EVAL_OWNER_PRINCIPAL_ID);
		assertTrue(authorizationManager.canAccess(userInfo, aa.getId().toString(), ObjectType.ACCESS_APPROVAL, ACCESS_TYPE.READ));
	}
	
	@Test
	public void testCanCreateEntityAccessRequirement() throws Exception {
		AccessRequirement ar = createEntityAccessRequirement();
		assertFalse(authorizationManager.canCreateAccessRequirement(userInfo, ar));
		userInfo.getGroups().add(actTeam); 
		assertTrue(authorizationManager.canCreateAccessRequirement(userInfo, ar));
		userInfo.getGroups().remove(actTeam);
		assertFalse(authorizationManager.canCreateAccessRequirement(userInfo, ar));
		// give user edit ability on entity 101
		when(mockNodeInheritanceDAO.getBenefactor("101")).thenReturn("101");
		when(mockAccessControlListDAO.canAccess(eq(userInfo.getGroups()), eq("101"), any(ACCESS_TYPE.class))).thenReturn(true);		
		assertTrue(authorizationManager.canCreateAccessRequirement(userInfo, ar));
	}
	
	@Test
	public void testCanAccessEntityAccessApprovalsForSubject() throws Exception {
		AccessApproval aa = createEntityAccessApproval();
		assertFalse(authorizationManager.canAccessAccessApprovalsForSubject(userInfo, createEntitySubjectId(), ACCESS_TYPE.READ));
		userInfo.getGroups().add(actTeam);
		assertTrue(authorizationManager.canAccessAccessApprovalsForSubject(userInfo, createEntitySubjectId(), ACCESS_TYPE.READ));
	}
	
	@Test
	public void testCanAccessEvaluationAccessApprovalsForSubject() throws Exception {
		AccessApproval aa = createEvaluationAccessApproval();
		assertFalse(authorizationManager.canAccessAccessApprovalsForSubject(userInfo, createEvaluationSubjectId(), ACCESS_TYPE.READ));
		userInfo.getIndividualGroup().setId(EVAL_OWNER_PRINCIPAL_ID);
		assertTrue(authorizationManager.canAccessAccessApprovalsForSubject(userInfo, createEvaluationSubjectId(), ACCESS_TYPE.READ));
	}
	
}
