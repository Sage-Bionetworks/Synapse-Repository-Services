package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.docker.RegistryEventAction.pull;
import static org.sagebionetworks.repo.model.docker.RegistryEventAction.push;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.download;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.evaluation.EvaluationPermissionsManager;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationManager;
import org.sagebionetworks.repo.manager.file.FileHandleAuthorizationStatus;
import org.sagebionetworks.repo.manager.team.TeamConstants;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DockerNodeDao;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.HasAccessorRequirement;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.SelfSignAccessRequirement;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.dbo.verification.VerificationDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AuthorizationManagerImplUnitTest {

	@Mock
	private DockerNodeDao mockDockerNodeDao;
	@Mock
	private AccessRequirementDAO  mockAccessRequirementDAO;
	@Mock
	private ActivityDAO mockActivityDAO;
	@Mock
	private FileHandleDao mockFileHandleDao;
	@Mock
	private EntityAuthorizationManager mockEntityAuthorizationManager;
	@Mock
	private AccessControlListDAO mockAclDAO;
	@Mock
	private FileHandleAssociationManager mockFileHandleAssociationManager;
	@Mock
	private VerificationDAO mockVerificationDao;
	@Mock
	private NodeDAO mockNodeDao;
	@Mock
	private DiscussionThreadDAO mockThreadDao;
	@Mock
	private ForumDAO mockForumDao;
	@Mock
	private V2WikiPageDao mockWikiPageDaoV2;
	@Mock
	private org.sagebionetworks.evaluation.dao.SubmissionDAO mockSubmissionDAO;
	@Mock
	private Submission mockSubmission;
	@Mock
	private EvaluationPermissionsManager mockEvaluationPermissionsManager;
	@Mock
	private MessageManager mockMessageManager;
	@Mock
	private org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO mockDataAccessSubmissionDao;
	@Mock
	private UserInfo mockACTUser;
	@Mock
	private GroupMembersDAO mockGroupMembersDao;
	@Mock
	private Set<String> accessors;
	@Mock
	private TokenGenerator mockTokenGenerator;

	@InjectMocks
	private AuthorizationManagerImpl authorizationManager;


	private static String USER_PRINCIPAL_ID = "123";
	private static String EVAL_OWNER_PRINCIPAL_ID = "987";
	private static String EVAL_ID = "1234567";
	
	private static final long PARENT_ID_LONG = 98765L;
	private static final String PARENT_ID = "syn"+PARENT_ID_LONG;
	
	private static final long REPO_ENTITY_ID_LONG = 123456L;
	private static final String REPO_ENTITY_ID = "syn"+REPO_ENTITY_ID_LONG;
	
	private static final long USER_ID = 111L;

	private static final UserInfo USER_INFO = new UserInfo(false, USER_ID);
	private static final UserInfo ADMIN_INFO = new UserInfo(true, 1L);
	
	private static final String REGISTRY_HOST = "docker.synapse.org";
	private static final String SERVICE = REGISTRY_HOST;
	private static final String REPOSITORY_PATH = PARENT_ID+"/reponame";
	private static final String REPOSITORY_NAME = SERVICE+"/"+REPOSITORY_PATH;
	private static final String ACCESS_TYPES_STRING="push,pull";
	
	private static final String REPOSITORY_TYPE = "repository";
	private static final String REGISTRY_TYPE = "registry";
	private static final String CATALOG_NAME = "catalog";
	private static final String ALL_ACCESS_TYPES = "*";
	
	private static final List<OAuthScope> OAUTH_SCOPES = ImmutableList.of(download, modify);

	private UserInfo userInfo;
	private UserInfo anonymousUserInfo;
	private UserInfo adminUser;
	private Evaluation evaluation;
	private String threadId;
	private String forumId;
	private String projectId;
	private DiscussionThreadBundle bundle;
	private Forum forum;
	private String submissionId;

	HasAccessorRequirement req;


	@BeforeEach
	public void setUp() throws Exception {
		userInfo = new UserInfo(false, USER_PRINCIPAL_ID);
		adminUser = new UserInfo(true, 456L);
		
		anonymousUserInfo = new UserInfo(false, BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());

		evaluation = new Evaluation();
		evaluation.setId(EVAL_ID);
		evaluation.setOwnerId(EVAL_OWNER_PRINCIPAL_ID);

		List<ACCESS_TYPE> participateAndDownload = new ArrayList<ACCESS_TYPE>();
		participateAndDownload.add(ACCESS_TYPE.DOWNLOAD);
		participateAndDownload.add(ACCESS_TYPE.PARTICIPATE);

		when(mockFileHandleAssociationManager.getAuthorizationObjectTypeForAssociatedObjectType(FileHandleAssociateType.FileEntity)).thenReturn(ObjectType.ENTITY);
		when(mockFileHandleAssociationManager.getAuthorizationObjectTypeForAssociatedObjectType(FileHandleAssociateType.TableEntity)).thenReturn(ObjectType.ENTITY);
		when(mockFileHandleAssociationManager.getAuthorizationObjectTypeForAssociatedObjectType(FileHandleAssociateType.WikiAttachment)).thenReturn(ObjectType.WIKI);

		threadId = "0";
		forumId = "1";
		projectId = "syn123";
		bundle = new DiscussionThreadBundle();
		bundle.setForumId(forumId);
		bundle.setProjectId(projectId);
		forum = new Forum();
		forum.setId(forumId);
		forum.setProjectId(projectId);
		when(mockThreadDao.getThread(Mockito.anyLong(), Mockito.any(DiscussionFilter.class))).thenReturn(bundle);

		submissionId = "111";

		Set<Long> groups = new HashSet<Long>();
		groups.add(TeamConstants.ACT_TEAM_ID);
		when(mockACTUser.getGroups()).thenReturn(groups);
		
		when(mockNodeDao.getNodeTypeById(PARENT_ID)).thenReturn(EntityType.project);
		
		when(mockDockerNodeDao.getEntityIdForRepositoryName(REPOSITORY_NAME)).thenReturn(REPO_ENTITY_ID);
		when(mockNodeDao.getBenefactor(REPO_ENTITY_ID)).thenReturn(REPO_ENTITY_ID);  // mocked to return something other than trash can
		
		when(mockEntityAuthorizationManager.hasAccess(eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ACCESS_TYPE.READ))).
		thenReturn(AuthorizationStatus.authorized());
	
		when(mockEntityAuthorizationManager.hasAccess(eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ACCESS_TYPE.DOWNLOAD))).
		thenReturn(AuthorizationStatus.authorized());
	
		when(mockEntityAuthorizationManager.hasAccess(eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ACCESS_TYPE.UPDATE))).
			thenReturn(AuthorizationStatus.authorized());
			
		when(mockEvaluationPermissionsManager.
				isDockerRepoNameInEvaluationWithAccess(anyString(), (Set<Long>)any(), (ACCESS_TYPE)any())).
				thenReturn(false);

		req = new SelfSignAccessRequirement();
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
		act.setCreatedBy(adminUser.getId().toString());
		when(mockActivityDAO.get(actId)).thenReturn(act);
		PaginatedResults<Reference> results1 = generateQueryResults(limit, total);
		PaginatedResults<Reference> results2 = generateQueryResults(total-limit, total);		
		PaginatedResults<Reference> results3 = generateQueryResults(total-(2*limit), total);
		when(mockActivityDAO.getEntitiesGeneratedBy(actId, limit, offset)).thenReturn(results1);
		when(mockActivityDAO.getEntitiesGeneratedBy(actId, limit, offset+limit)).thenReturn(results2);		
		when(mockActivityDAO.getEntitiesGeneratedBy(actId, limit, offset+(2*limit))).thenReturn(results3);

		boolean canAccess = authorizationManager.canAccessActivity(userInfo, actId).isAuthorized();
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
		act.setCreatedBy(adminUser.getId().toString());
		when(mockActivityDAO.get(actId)).thenReturn(act);
		PaginatedResults<Reference> results1 = generateQueryResults(1, 1);		
		when(mockActivityDAO.getEntitiesGeneratedBy(actId, limit, offset)).thenReturn(results1);		

		boolean canAccess = authorizationManager.canAccessActivity(userInfo, actId).isAuthorized();
		verify(mockActivityDAO).getEntitiesGeneratedBy(actId, limit, offset);
		assertFalse(canAccess);
	}

	@Test
	public void testCanAccessRawFileHandleByCreator(){
		// The admin can access anything
		String creator = userInfo.getId().toString();
		assertTrue(authorizationManager.canAccessRawFileHandleByCreator(adminUser, "101", creator).isAuthorized(), "Admin should have access to all FileHandles");
		assertTrue(authorizationManager.canAccessRawFileHandleByCreator(userInfo, "101", creator).isAuthorized(), "Creator should have access to their own FileHandles");
		// Set the creator to be the admin this time.
		creator = adminUser.getId().toString();
		assertFalse(authorizationManager.canAccessRawFileHandleByCreator(userInfo, "101", creator).isAuthorized(), "Only the creator (or admin) should have access a FileHandle");
	}

	@Test
	public void testCanAccessRawFileHandleById() throws NotFoundException{
		// The admin can access anything
		String creator = userInfo.getId().toString();
		String fileHandlId = "3333";
		when(mockFileHandleDao.getHandleCreator(fileHandlId)).thenReturn(creator);
		assertTrue(authorizationManager.canAccessRawFileHandleById(adminUser, fileHandlId).isAuthorized(), "Admin should have access to all FileHandles");
		assertTrue(authorizationManager.canAccessRawFileHandleById(userInfo, fileHandlId).isAuthorized(), "Creator should have access to their own FileHandles");
		// change the users id
		UserInfo notTheCreatoro = new UserInfo(false, "999999");
		assertFalse(authorizationManager.canAccessRawFileHandleById(notTheCreatoro, fileHandlId).isAuthorized(), "Only the creator (or admin) should have access a FileHandle");
		verify(mockFileHandleDao, times(2)).getHandleCreator(fileHandlId);
	}

	@Test
	public void testCanAccessWithObjectTypeEntityAllow() throws DatastoreException, NotFoundException{
		String entityId = "syn123";
		when(mockEntityAuthorizationManager.hasAccess(eq(userInfo), any(String.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		assertTrue(authorizationManager.canAccess(userInfo, entityId, ObjectType.ENTITY, ACCESS_TYPE.DELETE).isAuthorized(), "User should have acces to do anything with this entity");
	}

	@Test
	public void testCanAccessWithObjectTypeEntityDeny() throws DatastoreException, NotFoundException{
		String entityId = "syn123";
		when(mockEntityAuthorizationManager.hasAccess(eq(userInfo), any(String.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
		assertFalse(authorizationManager.canAccess(userInfo, entityId, ObjectType.ENTITY, ACCESS_TYPE.DELETE).isAuthorized(), "User should not have acces to do anything with this entity");
	}

	@Test
	public void testCanAccessWithTrashCanException() throws DatastoreException, NotFoundException{
		when(mockEntityAuthorizationManager.hasAccess(eq(userInfo), eq("syn123"), any(ACCESS_TYPE.class))).thenThrow(new EntityInTrashCanException(""));
		assertThrows(EntityInTrashCanException.class, ()-> {
			authorizationManager.canAccess(userInfo, "syn123", ObjectType.ENTITY, ACCESS_TYPE.READ);
		});
	}

	@Test
	public void testVerifyACTTeamMembershipOrIsAdmin_Admin() {
		UserInfo adminInfo = new UserInfo(true);
		assertTrue(authorizationManager.isACTTeamMemberOrAdmin(adminInfo));
	}
	
	@Test
	public void testVerifyACTTeamMembershipOrIsAdminNullGroups() {
		UserInfo adminInfo = new UserInfo(false);
		assertFalse(authorizationManager.isACTTeamMemberOrAdmin(adminInfo));
	}

	@Test
	public void testVerifyACTTeamMembershipOrIsAdmin_ACT() {
		userInfo.getGroups().add(TeamConstants.ACT_TEAM_ID);
		assertTrue(authorizationManager.isACTTeamMemberOrAdmin(userInfo));
	}

	@Test
	public void testVerifyACTTeamMembershipOrIsAdmin_NONE() {
		assertFalse(authorizationManager.isACTTeamMemberOrAdmin(userInfo));
	}

	@Test
	public void testVerifyReportTeamMembershipOrIsAdmin_Admin() {
		UserInfo adminInfo = new UserInfo(true);
		assertTrue(authorizationManager.isReportTeamMemberOrAdmin(adminInfo));
	}

	@Test
	public void testVerifyReportTeamMembershipOrIsAdminNullGroups() {
		UserInfo adminInfo = new UserInfo(false);
		assertFalse(authorizationManager.isReportTeamMemberOrAdmin(adminInfo));
	}

	@Test
	public void testVerifyReportTeamMembershipOrIsAdmin_ReportTeam() {
		userInfo.getGroups().add(TeamConstants.SYNAPSE_REPORT_TEAM_ID);
		assertTrue(authorizationManager.isReportTeamMemberOrAdmin(userInfo));
	}

	@Test
	public void testVerifyReportTeamMembershipOrIsAdmin_NONE() {
		assertFalse(authorizationManager.isReportTeamMemberOrAdmin(userInfo));
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

	@Test
	public void testCanAccessEntityAccessRequirement() throws Exception {
		AccessRequirement ar = createEntityAccessRequirement();
		assertFalse(authorizationManager.canAccess(userInfo, ar.getId().toString(), ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.UPDATE).isAuthorized());
		userInfo.getGroups().add(TeamConstants.ACT_TEAM_ID);
		assertTrue(authorizationManager.canAccess(userInfo, "1234", ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.UPDATE).isAuthorized());
	}

	@Test
	public void testCanAccessEntityAccessRequirementWithDownload() throws Exception {
		AccessRequirement ar = createEntityAccessRequirement();
		assertTrue(authorizationManager.canAccess(userInfo, ar.getId().toString(), ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.DOWNLOAD).isAuthorized());
	}

	@Test
	public void testCanAccessEvaluationAccessRequirement() throws Exception {
		AccessRequirement ar = createEvaluationAccessRequirement();
		assertFalse(authorizationManager.canAccess(userInfo, ar.getId().toString(), ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.UPDATE).isAuthorized());
		userInfo.setId(Long.parseLong(EVAL_OWNER_PRINCIPAL_ID));
		// only ACT may update an access requirement
		assertFalse(authorizationManager.canAccess(userInfo, ar.getId().toString(), ObjectType.ACCESS_REQUIREMENT, ACCESS_TYPE.UPDATE).isAuthorized());
	}

	@Test
	public void testCanAccessEntityAccessApproval() throws Exception {
		assertFalse(authorizationManager.canAccess(userInfo, "1", ObjectType.ACCESS_APPROVAL, ACCESS_TYPE.READ).isAuthorized());
		userInfo.getGroups().add(TeamConstants.ACT_TEAM_ID);
		assertTrue(authorizationManager.canAccess(userInfo, "1", ObjectType.ACCESS_APPROVAL, ACCESS_TYPE.READ).isAuthorized());
	}

	@Test
	public void testCanAccessEntityAccessApprovalsForSubject() throws Exception {
		assertFalse(authorizationManager.canAccessAccessApprovalsForSubject(userInfo, createEntitySubjectId(), ACCESS_TYPE.READ).isAuthorized());
		userInfo.getGroups().add(TeamConstants.ACT_TEAM_ID);
		assertTrue(authorizationManager.canAccessAccessApprovalsForSubject(userInfo, createEntitySubjectId(), ACCESS_TYPE.READ).isAuthorized());
	}

	@Test
	public void testCanAccessEvaluationAccessApprovalsForSubject() throws Exception {
		assertFalse(authorizationManager.canAccessAccessApprovalsForSubject(userInfo, createEvaluationSubjectId(), ACCESS_TYPE.READ).isAuthorized());
		userInfo.setId(Long.parseLong(EVAL_OWNER_PRINCIPAL_ID));
		// only ACT may review access approvals
		assertFalse(authorizationManager.canAccessAccessApprovalsForSubject(userInfo, createEvaluationSubjectId(), ACCESS_TYPE.READ).isAuthorized());
	}
	
	@Test
	public void testCanAccessTeam() throws Exception {
		String teamId = "123";
		ACCESS_TYPE accessType = ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE;
		// admin can always access
		assertTrue(authorizationManager.canAccess(adminUser, teamId, ObjectType.TEAM, accessType).isAuthorized());
		// non admin can access if acl says so
		when(mockAclDAO.canAccess(userInfo.getGroups(), teamId, ObjectType.TEAM, accessType)).thenReturn(true);
		assertTrue(authorizationManager.canAccess(userInfo, teamId, ObjectType.TEAM, accessType).isAuthorized());
		// otherwise not
		when(mockAclDAO.canAccess(userInfo.getGroups(), teamId, ObjectType.TEAM, accessType)).thenReturn(false);
		assertFalse(authorizationManager.canAccess(userInfo, teamId, ObjectType.TEAM, accessType).isAuthorized());
	}

	@Test
	public void testCanDownloadTeamIcon() throws Exception {
		assertTrue(authorizationManager.canAccess(userInfo, "123", ObjectType.TEAM, ACCESS_TYPE.DOWNLOAD).isAuthorized());
	}

	@Test
	public void testCanAccessWiki() throws Exception {
		String wikiId = "1";
		String entityId = "syn123";
		WikiPageKey key = new WikiPageKey();
		key.setOwnerObjectId(entityId);
		key.setOwnerObjectType(ObjectType.ENTITY);
		when(mockWikiPageDaoV2.lookupWikiKey(wikiId)).thenReturn(key);
		when(mockEntityAuthorizationManager.hasAccess(userInfo, entityId, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		assertTrue(authorizationManager.canAccess(userInfo, wikiId, ObjectType.WIKI, ACCESS_TYPE.DOWNLOAD).isAuthorized());
	}
	
	@Test
	public void testCanAccessWikiUpdate() throws Exception {
		String wikiId = "1";
		String entityId = "syn123";
		WikiPageKey key = new WikiPageKey();
		key.setOwnerObjectId(entityId);
		key.setOwnerObjectType(ObjectType.ENTITY);
		when(mockWikiPageDaoV2.lookupWikiKey(wikiId)).thenReturn(key);
		when(mockEntityAuthorizationManager.hasAccess(userInfo, entityId, ACCESS_TYPE.UPDATE))
				.thenReturn(AuthorizationStatus.authorized());
		assertTrue(authorizationManager.canAccess(userInfo, wikiId, ObjectType.WIKI, ACCESS_TYPE.UPDATE).isAuthorized());
	}
	
	/**
	 * This test was added for PLFM-4689. Anonymous users must be able to download wiki attachments
	 * when the wiki is public read.
	 * @throws Exception
	 */
	@Test
	public void testCanAccessWikiAnonymous() throws Exception {
		String wikiId = "1";
		String entityId = "syn123";
		WikiPageKey key = new WikiPageKey();
		key.setOwnerObjectId(entityId);
		key.setOwnerObjectType(ObjectType.ENTITY);
		when(mockWikiPageDaoV2.lookupWikiKey(wikiId)).thenReturn(key);
		when(mockEntityAuthorizationManager.hasAccess(anonymousUserInfo, entityId, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		assertTrue(authorizationManager.canAccess(anonymousUserInfo, wikiId, ObjectType.WIKI, ACCESS_TYPE.DOWNLOAD).isAuthorized());
	}

	@Test
	public void testCanAccessUserProfile() throws Exception {
		assertTrue(authorizationManager.canAccess(userInfo, userInfo.getId().toString(), ObjectType.USER_PROFILE, ACCESS_TYPE.DOWNLOAD).isAuthorized());
		for (ACCESS_TYPE type : ACCESS_TYPE.values()) {
			if (!type.equals(ACCESS_TYPE.DOWNLOAD)) {
				assertFalse(authorizationManager.canAccess(userInfo, "1", ObjectType.USER_PROFILE, type).isAuthorized());
			}
		}
	}

	@Test
	public void testCanAccessEvaluationSubmissionAccessDenied() throws Exception {
		String submissionId = "1";
		String evalId = "2";
		when(mockSubmissionDAO.get(submissionId)).thenReturn(mockSubmission);
		when(mockSubmission.getEvaluationId()).thenReturn(evalId);
		when(mockEvaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.READ_PRIVATE_SUBMISSION)).thenReturn(AuthorizationStatus.accessDenied(""));
		assertFalse(authorizationManager.canAccess(userInfo, submissionId, ObjectType.EVALUATION_SUBMISSIONS, ACCESS_TYPE.DOWNLOAD).isAuthorized());
	}

	@Test
	public void testCanAccessEvaluationSubmissionAuthorized() throws Exception {
		String submissionId = "1";
		String evalId = "2";
		when(mockSubmissionDAO.get(submissionId)).thenReturn(mockSubmission);
		when(mockSubmission.getEvaluationId()).thenReturn(evalId);
		when(mockEvaluationPermissionsManager.hasAccess(userInfo, evalId, ACCESS_TYPE.READ_PRIVATE_SUBMISSION)).thenReturn(AuthorizationStatus.authorized());
		assertTrue(authorizationManager.canAccess(userInfo, submissionId, ObjectType.EVALUATION_SUBMISSIONS, ACCESS_TYPE.DOWNLOAD).isAuthorized());
	}

	@Test
	public void testCanAccessEvaluationSubmissionNonDownload() throws Exception {
		for (ACCESS_TYPE type : ACCESS_TYPE.values()) {
			if (!type.equals(ACCESS_TYPE.DOWNLOAD)) {
				assertFalse(authorizationManager.canAccess(userInfo, "1", ObjectType.EVALUATION_SUBMISSIONS, type).isAuthorized());
			}
		}
	}

	@Test
	public void testCanAccessMessageAccessDenied() throws Exception {
		String messageId = "1";
		when(mockMessageManager.getMessage(userInfo, messageId)).thenThrow(new UnauthorizedException());
		assertFalse(authorizationManager.canAccess(userInfo, messageId, ObjectType.MESSAGE, ACCESS_TYPE.DOWNLOAD).isAuthorized());
	}

	@Test
	public void testCanAccessMessageAuthorized() throws Exception {
		String messageId = "1";
		assertTrue(authorizationManager.canAccess(userInfo, messageId, ObjectType.MESSAGE, ACCESS_TYPE.DOWNLOAD).isAuthorized());
	}

	@Test
	public void testCanAccessMessageNonDownload() throws Exception {
		for (ACCESS_TYPE type : ACCESS_TYPE.values()) {
			if (!type.equals(ACCESS_TYPE.DOWNLOAD)) {
				assertFalse(authorizationManager.canAccess(userInfo, "1", ObjectType.MESSAGE, type).isAuthorized());
			}
		}
	}

	@Test
	public void testCanAccessDataAccessRequestUnauthorized() throws Exception {
		assertFalse(authorizationManager.canAccess(userInfo, "1", ObjectType.DATA_ACCESS_REQUEST, ACCESS_TYPE.DOWNLOAD).isAuthorized());
	}

	@Test
	public void testCanAccessDataAccessRequestAuthorized() throws Exception {
		assertTrue(authorizationManager.canAccess(mockACTUser, "1", ObjectType.DATA_ACCESS_REQUEST, ACCESS_TYPE.DOWNLOAD).isAuthorized());
	}

	@Test
	public void testCanAccessDataAccessRequestNonDownload() throws Exception {
		for (ACCESS_TYPE type : ACCESS_TYPE.values()) {
			if (!type.equals(ACCESS_TYPE.DOWNLOAD)) {
				assertFalse(authorizationManager.canAccess(userInfo, "1", ObjectType.DATA_ACCESS_REQUEST, type).isAuthorized());
			}
		}
	}

	@Test
	public void testCanAccessDataAccessSubmissionUnauthorized() throws Exception {
		assertFalse(authorizationManager.canAccess(userInfo, "1", ObjectType.DATA_ACCESS_SUBMISSION, ACCESS_TYPE.DOWNLOAD).isAuthorized());
	}

	@Test
	public void testCanAccessDataAccessSubmissionAuthorized() throws Exception {
		assertTrue(authorizationManager.canAccess(mockACTUser, "1", ObjectType.DATA_ACCESS_SUBMISSION, ACCESS_TYPE.DOWNLOAD).isAuthorized());
	}

	@Test
	public void testCanAccessDataAccessSubmissionNonDownload() throws Exception {
		for (ACCESS_TYPE type : ACCESS_TYPE.values()) {
			if (!type.equals(ACCESS_TYPE.DOWNLOAD)) {
				assertFalse(authorizationManager.canAccess(userInfo, "1", ObjectType.DATA_ACCESS_SUBMISSION, type).isAuthorized());
			}
		}
	}
	
	@Test
	public void testCanDownloadVerificationSubmission() throws Exception {
		String verificationId = "123";
		long verificationIdLong = Long.parseLong(verificationId);
		ACCESS_TYPE accessType = ACCESS_TYPE.DOWNLOAD;
		ObjectType ot = ObjectType.VERIFICATION_SUBMISSION;
		// admin can always access
		assertTrue(authorizationManager.canAccess(adminUser, verificationId, ot, accessType).isAuthorized());
		
		// owner can access
		when(mockVerificationDao.getVerificationSubmitter(verificationIdLong)).thenReturn(userInfo.getId());
		assertTrue(authorizationManager.canAccess(userInfo, verificationId, ot, accessType).isAuthorized());
		
		// non-owner can't access
		when(mockVerificationDao.getVerificationSubmitter(verificationIdLong)).thenReturn(userInfo.getId()*13);
		assertFalse(authorizationManager.canAccess(userInfo, verificationId, ot, accessType).isAuthorized());
		
		// ACT can access
		UserInfo actInfo = new UserInfo(false);
		actInfo.setId(999L);
		actInfo.setGroups(Collections.singleton(TeamConstants.ACT_TEAM_ID));
		when(mockVerificationDao.getVerificationSubmitter(verificationIdLong)).thenReturn(userInfo.getId()*13);
		assertTrue(authorizationManager.canAccess(actInfo, verificationId, ot, accessType).isAuthorized());
		
		// can't do other operations though
		assertFalse(authorizationManager.canAccess(actInfo, verificationId, ot, ACCESS_TYPE.UPDATE).isAuthorized());

	}
	
	private static void addEntityHeaderTo(String id, Collection<EntityHeader>c) {
		EntityHeader h = new EntityHeader(); 
		h.setId(id); 
		c.add(h);
	}
	
	@Test
	public void testCanMoveEntity() throws Exception {
		// mock nodeDao
		String parentId = "syn12345";
		List<Long> ancestorIds = new ArrayList<Long>();
		ancestorIds.add(KeyFactory.stringToKey(parentId));
		ancestorIds.add(999L);
		when(mockNodeDao.getEntityPathIds(parentId)).thenReturn(ancestorIds);
		
		String newParentId = "syn6789";
		List<Long> newAncestorIds = new ArrayList<Long>();
		newAncestorIds.add(KeyFactory.stringToKey(newParentId));
		newAncestorIds.add(888L);
		when(mockNodeDao.getEntityPathIds(newParentId)).thenReturn(newAncestorIds);
		
		List<String> diff = Arrays.asList("1");
		when(mockAccessRequirementDAO.getAccessRequirementDiff(ancestorIds, newAncestorIds, RestrictableObjectType.ENTITY)).thenReturn(new LinkedList<String>());
		when(mockNodeDao.isNodeAvailable(parentId)).thenReturn(true);
		
		// since 'ars' list doesn't change, will return true
		assertTrue(authorizationManager.canUserMoveRestrictedEntity(userInfo, parentId, newParentId).isAuthorized());
		verify(mockNodeDao).getEntityPathIds(parentId);
		verify(mockAccessRequirementDAO).getAccessRequirementDiff(ancestorIds, newAncestorIds, RestrictableObjectType.ENTITY);

		// but making less restrictive is NOT OK
		when(mockAccessRequirementDAO.getAccessRequirementDiff(ancestorIds, newAncestorIds, RestrictableObjectType.ENTITY)).thenReturn(diff);
		assertFalse(authorizationManager.canUserMoveRestrictedEntity(userInfo, parentId, newParentId).isAuthorized());
		
		// but if the user is an admin, will be true
		assertTrue(authorizationManager.canUserMoveRestrictedEntity(adminUser, parentId, newParentId).isAuthorized());
	}
	
	@Test
	public void testCanDownloadFileAdmin(){
		List<String> fileHandleIds = Arrays.asList("1","2");
		String associatedObjectId = "456";
		FileHandleAssociateType associationType = FileHandleAssociateType.TableEntity;
		// call under test.
		List<FileHandleAuthorizationStatus> results = authorizationManager.canDownloadFile(adminUser, fileHandleIds, associatedObjectId, associationType);
		assertNotNull(results);
		List<FileHandleAuthorizationStatus> expected = Arrays.asList(
				new FileHandleAuthorizationStatus("1", AuthorizationStatus.authorized()),
				new FileHandleAuthorizationStatus("2", AuthorizationStatus.authorized())
		);
		
		assertEquals(expected, results);
	}
	
	@Test
	public void testCanDownloadFileFileCreator(){
		List<String> fileHandleIds = Arrays.asList("1","2");
		String associatedObjectId = "456";
		FileHandleAssociateType associationType = FileHandleAssociateType.TableEntity;
		//the user is not authorized to download the associated object.
		when(mockEntityAuthorizationManager.hasAccess(userInfo, associatedObjectId, ACCESS_TYPE.DOWNLOAD)).thenReturn(AuthorizationStatus.accessDenied("cause"));
		// user is the creator of "2"
		when(mockFileHandleDao.getFileHandleIdsCreatedByUser(userInfo.getId(), fileHandleIds)).thenReturn(Sets.newHashSet("2"));
		// neither file is associated with the object.
		Set<String> emptySet = Sets.newHashSet();
		when(mockFileHandleAssociationManager.getFileHandleIdsAssociatedWithObject(fileHandleIds, associatedObjectId, associationType)).thenReturn(emptySet);
		// call under test.
		List<FileHandleAuthorizationStatus> results = authorizationManager.canDownloadFile(userInfo, fileHandleIds, associatedObjectId, associationType);
		assertNotNull(results);
		List<FileHandleAuthorizationStatus> expected = Arrays.asList(
				new FileHandleAuthorizationStatus("1", AuthorizationManagerImpl.accessDeniedFileNotAssociatedWithObject("1", associatedObjectId,
						associationType)),
						// The user is the creator of this file so they can download it.
				new FileHandleAuthorizationStatus("2", AuthorizationStatus.authorized())
		);
		
		assertEquals(expected, results);
	}
	
	@Test
	public void testCanDownloadFileFileNotAssociated(){
		List<String> fileHandleIds = Arrays.asList("1","2");
		String associatedObjectId = "456";
		FileHandleAssociateType associationType = FileHandleAssociateType.TableEntity;
		Set<String> emptySet = Sets.newHashSet();
		// the user has download on the associated object.
		when(mockEntityAuthorizationManager.hasAccess(userInfo, associatedObjectId, ACCESS_TYPE.DOWNLOAD)).thenReturn(AuthorizationStatus.authorized());
		// user is not the creator of either file.
		when(mockFileHandleDao.getFileHandleIdsCreatedByUser(userInfo.getId(), fileHandleIds)).thenReturn(emptySet);
		// neither file is associated with the object.
		when(mockFileHandleAssociationManager.getFileHandleIdsAssociatedWithObject(fileHandleIds, associatedObjectId, associationType)).thenReturn(emptySet);
		// call under test.
		List<FileHandleAuthorizationStatus> results = authorizationManager.canDownloadFile(userInfo, fileHandleIds, associatedObjectId, associationType);
		assertNotNull(results);
		List<FileHandleAuthorizationStatus> expected = Arrays.asList(
				new FileHandleAuthorizationStatus("1", AuthorizationManagerImpl.accessDeniedFileNotAssociatedWithObject("1", associatedObjectId,
						associationType)),
				new FileHandleAuthorizationStatus("2", AuthorizationManagerImpl.accessDeniedFileNotAssociatedWithObject("2", associatedObjectId,
						associationType))
		);
		assertEquals(expected, results);
	}
	
	@Test
	public void testCanDownloadFileAuthorized(){
		List<String> fileHandleIds = Arrays.asList("1","2");
		String associatedObjectId = "456";
		FileHandleAssociateType associationType = FileHandleAssociateType.TableEntity;
		Set<String> emptySet = Sets.newHashSet();
		//the user is authorized to download the associated object.
		when(mockEntityAuthorizationManager.hasAccess(userInfo, associatedObjectId, ACCESS_TYPE.DOWNLOAD)).thenReturn(AuthorizationStatus.authorized());
		// user is not the creator of either file.
		when(mockFileHandleDao.getFileHandleIdsCreatedByUser(userInfo.getId(), fileHandleIds)).thenReturn(emptySet);
		// the first file is associated with the object.
		Set<String> bothFileHandlIds = Sets.newHashSet("1");
		when(mockFileHandleAssociationManager.getFileHandleIdsAssociatedWithObject(fileHandleIds, associatedObjectId, associationType)).thenReturn(bothFileHandlIds);
		// call under test.
		List<FileHandleAuthorizationStatus> results = authorizationManager.canDownloadFile(userInfo, fileHandleIds, associatedObjectId, associationType);
		assertNotNull(results);
		List<FileHandleAuthorizationStatus> expected = Arrays.asList(
				new FileHandleAuthorizationStatus("1", AuthorizationStatus.authorized()),
				new FileHandleAuthorizationStatus("2", AuthorizationManagerImpl.accessDeniedFileNotAssociatedWithObject("2", associatedObjectId,
						associationType))
		);
		assertEquals(expected, results);
	}
	
	@Test
	public void testCanDownloadFileUnAuthorized(){
		List<String> fileHandleIds = Arrays.asList("1","2");
		String associatedObjectId = "456";
		FileHandleAssociateType associationType = FileHandleAssociateType.TableEntity;
		Set<String> emptySet = Sets.newHashSet();
		//the user is not authorized to download the associated object.
		when(mockEntityAuthorizationManager.hasAccess(userInfo, associatedObjectId, ACCESS_TYPE.DOWNLOAD)).thenReturn(AuthorizationStatus.accessDenied("cause"));
		// user is not the creator of either file.
		when(mockFileHandleDao.getFileHandleIdsCreatedByUser(userInfo.getId(), fileHandleIds)).thenReturn(emptySet);
		// the first file is associated with the object.
		Set<String> bothFileHandlIds = Sets.newHashSet("1");
		when(mockFileHandleAssociationManager.getFileHandleIdsAssociatedWithObject(fileHandleIds, associatedObjectId, associationType)).thenReturn(bothFileHandlIds);
		// call under test.
		List<FileHandleAuthorizationStatus> results = authorizationManager.canDownloadFile(userInfo, fileHandleIds, associatedObjectId, associationType);
		assertNotNull(results);
		List<FileHandleAuthorizationStatus> expected = Arrays.asList(
				new FileHandleAuthorizationStatus("1", AuthorizationStatus.accessDenied("cause")),
				new FileHandleAuthorizationStatus("2", AuthorizationManagerImpl.accessDeniedFileNotAssociatedWithObject("2", associatedObjectId,
						associationType))
		);
		assertEquals(expected, results);
	}
	
	@Test
	public void testCanReadBenefactorsAdmin(){
		Set<Long> benefactors = Sets.newHashSet(1L,2L);
		// call under test
		Set<Long> results = authorizationManager.getAccessibleBenefactors(adminUser, ObjectType.ENTITY, benefactors);
		assertEquals(benefactors, results);
		verify(mockAclDAO, never()).getAccessibleBenefactors(any(Set.class), any(Set.class), any(ObjectType.class), any(ACCESS_TYPE.class));
	}
	
	@Test
	public void testCanReadBenefactorsNonAdmin(){
		Set<Long> benefactors = Sets.newHashSet(1L,2L);
		// call under test
		authorizationManager.getAccessibleBenefactors(userInfo, ObjectType.ENTITY, benefactors);
		verify(mockAclDAO, times(1)).getAccessibleBenefactors(any(Set.class), any(Set.class), any(ObjectType.class), any(ACCESS_TYPE.class));
	}
	
	@Test
	public void testCanReadBenefactorsTrashAdmin(){
		Set<Long> benefactors = Sets.newHashSet(AuthorizationManagerImpl.TRASH_FOLDER_ID);
		// call under test
		Set<Long> results = authorizationManager.getAccessibleBenefactors(adminUser, ObjectType.ENTITY, benefactors);
		assertNotNull(results);
		assertEquals(0, results.size());
	}
	
	@Test
	public void testCanReadBenefactorsTrashNonAdmin(){
		Set<Long> benefactors = Sets.newHashSet(AuthorizationManagerImpl.TRASH_FOLDER_ID);
		when(mockAclDAO.getAccessibleBenefactors(any(Set.class), any(Set.class), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(benefactors);
		// call under test
		Set<Long> results = authorizationManager.getAccessibleBenefactors(userInfo, ObjectType.ENTITY, benefactors);
		assertNotNull(results);
		assertEquals(0, results.size());
	}

	@Test
	public void testCanSubscribeForumUnauthorized() {
		when(mockEntityAuthorizationManager.hasAccess(userInfo, projectId, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.accessDenied(""));
		when(mockForumDao.getForum(Long.parseLong(forumId))).thenReturn(forum);
		assertEquals(AuthorizationStatus.accessDenied(""),
				authorizationManager.canSubscribe(userInfo, forumId, SubscriptionObjectType.FORUM));
	}

	@Test
	public void testCanSubscribeForumAuthorized() {
		when(mockEntityAuthorizationManager.hasAccess(userInfo, projectId, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		when(mockForumDao.getForum(Long.parseLong(forumId))).thenReturn(forum);
		assertEquals(AuthorizationStatus.authorized(),
				authorizationManager.canSubscribe(userInfo, forumId, SubscriptionObjectType.FORUM));
	}

	@Test
	public void testCanSubscribeThreadUnauthorized() {
		when(mockEntityAuthorizationManager.hasAccess(userInfo, projectId, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.accessDenied(""));
		when(mockThreadDao.getProjectId(threadId)).thenReturn(projectId);
		assertEquals(AuthorizationStatus.accessDenied(""),
				authorizationManager.canSubscribe(userInfo, threadId, SubscriptionObjectType.THREAD));
	}

	@Test
	public void testCanSubscribeThreadAuthorized() {
		when(mockEntityAuthorizationManager.hasAccess(userInfo, projectId, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		when(mockThreadDao.getProjectId(threadId)).thenReturn(projectId);
		assertEquals(AuthorizationStatus.authorized(),
				authorizationManager.canSubscribe(userInfo, threadId, SubscriptionObjectType.THREAD));
	}

	@Test
	public void testCanSubscribeDataAccessSubmissionUnauthorized() {
		assertFalse(authorizationManager.canSubscribe(userInfo, submissionId, SubscriptionObjectType.DATA_ACCESS_SUBMISSION).isAuthorized());
	}

	@Test
	public void testCanSubscribeDataAccessSubmissionAdminAuthorized() {
		assertEquals(AuthorizationStatus.authorized(),
				authorizationManager.canSubscribe(adminUser, submissionId, SubscriptionObjectType.DATA_ACCESS_SUBMISSION));
	}

	@Test
	public void testCanSubscribeDataAccessSubmissionACTAuthorized() {
		assertEquals(AuthorizationStatus.authorized(),
				authorizationManager.canSubscribe(mockACTUser, submissionId, SubscriptionObjectType.DATA_ACCESS_SUBMISSION));
	}

	@Test
	public void testCanSubscribeDataAccessSubmissionStatusUnauthorized() {
		when(mockDataAccessSubmissionDao.isAccessor(submissionId, userInfo.getId().toString()))
				.thenReturn(false);
		assertFalse(authorizationManager.canSubscribe(userInfo, submissionId, SubscriptionObjectType.DATA_ACCESS_SUBMISSION_STATUS).isAuthorized());
	}

	@Test
	public void testCanSubscribeDataAccessSubmissionStatusAuthorized() {
		when(mockDataAccessSubmissionDao.isAccessor(submissionId, userInfo.getId().toString()))
				.thenReturn(true);
		assertEquals(AuthorizationStatus.authorized(),
				authorizationManager.canSubscribe(userInfo, submissionId, SubscriptionObjectType.DATA_ACCESS_SUBMISSION_STATUS));
	}

	@Test
	public void testGetAccessibleProjectIds(){
		Set<Long> expectedProjectIds = Sets.newHashSet(555L);
		Set<Long> principalIds = Sets.newHashSet(123L);
		when(mockAclDAO.getAccessibleProjectIds(principalIds, ACCESS_TYPE.READ)).thenReturn(expectedProjectIds);
		Set<Long> results = authorizationManager.getAccessibleProjectIds(principalIds);
		assertEquals(expectedProjectIds,results);
	}
	
	@Test
	public void testGetAccessibleProjectIdsEmpty(){
		Set<Long> principalIds = new HashSet<>();
		Set<Long> results = authorizationManager.getAccessibleProjectIds(principalIds);
		assertNotNull(results);
		assertTrue(results.isEmpty());
		verify(mockAclDAO, never()).getAccessibleProjectIds(any(Set.class), any(ACCESS_TYPE.class));
	}
	
	@Test
	public void testGetAccessibleProjectIdsNullPrincipals(){
		Set<Long> principalIds = null;
		assertThrows(IllegalArgumentException.class, ()-> {
			authorizationManager.getAccessibleProjectIds(principalIds);
		});
	}

	@Test
	public void testValidParentProjectIdInvalidRepoName() {
		assertEquals(null, authorizationManager.validDockerRepositoryParentId("/invalid/"));
	}

	@Test
	public void testValidParentProjectIdInvalidSynID() {
		assertEquals(null, authorizationManager.validDockerRepositoryParentId("uname/myrepo"));
	}

	@Test
	public void testValidParentProjectIdParentNotAProject() {
		when(mockNodeDao.getNodeTypeById(PARENT_ID)).thenReturn(EntityType.folder);
		assertEquals(null, authorizationManager.validDockerRepositoryParentId(PARENT_ID+"/myrepo"));
	}

	@Test
	public void testValidParentProjectIdHappyPath() {
		assertEquals(PARENT_ID, authorizationManager.validDockerRepositoryParentId(PARENT_ID+"/myrepo"));
	}
	
	
	@Test
	public void testGetPermittedAccessTypesNullUserInfo() throws Exception{
		assertThrows(IllegalArgumentException.class, ()-> {
			authorizationManager.getPermittedDockerActions(null, OAUTH_SCOPES, SERVICE, REPOSITORY_TYPE, REPOSITORY_PATH, ACCESS_TYPES_STRING);
		});
	}
	
	@Test
	public void testGetPermittedAccessTypesNullService() throws Exception{
		assertThrows(IllegalArgumentException.class, ()-> {
			authorizationManager.getPermittedDockerActions(USER_INFO, OAUTH_SCOPES, null, REPOSITORY_TYPE, REPOSITORY_PATH, ACCESS_TYPES_STRING);
		});
	}
	
	@Test
	public void testGetPermittedAccessTypesNullRepositoryPath() throws Exception{
		assertThrows(IllegalArgumentException.class, ()-> {
			authorizationManager.getPermittedDockerActions(USER_INFO, OAUTH_SCOPES, SERVICE, REPOSITORY_TYPE, null, ACCESS_TYPES_STRING);
		});
	}
	
	@Test
	public void testGetPermittedAccessTypesNullAction() throws Exception{
		assertThrows(IllegalArgumentException.class, ()-> {
			authorizationManager.getPermittedDockerActions(USER_INFO, OAUTH_SCOPES, SERVICE, REPOSITORY_TYPE, REPOSITORY_PATH, null);
		});
	}
	
	@Test
	public void testGetPermittedAccessTypesHappyCase() throws Exception {
		// method under test:
		Set<String> permitted = authorizationManager.
				getPermittedDockerActions(USER_INFO, OAUTH_SCOPES, SERVICE, REPOSITORY_TYPE, REPOSITORY_PATH, ACCESS_TYPES_STRING);
		
		assertEquals(new HashSet(Arrays.asList(new String[]{push.name(), pull.name()})), permitted);
	}

	@Test
	public void testGetPermittedAccessTypesLimitedScope() throws Exception {
		List<OAuthScope> downloadOnlyScope = Collections.singletonList(OAuthScope.download);
		
		// method under test:
		Set<String> permitted = authorizationManager.
				getPermittedDockerActions(USER_INFO, downloadOnlyScope, SERVICE, REPOSITORY_TYPE, REPOSITORY_PATH, ACCESS_TYPES_STRING);
		
		// user has permission to read and write, but scope is limited to view, so only 'pull' is granted
		assertEquals(new HashSet(Arrays.asList(new String[]{pull.name()})), permitted);
	}

	@Test
	public void testGetPermittedAccessTypesRegistry() throws Exception {
		// method under test:
		Set<String> permitted = authorizationManager.
				getPermittedDockerActions(ADMIN_INFO, Collections.singletonList(OAuthScope.view), SERVICE, REGISTRY_TYPE, CATALOG_NAME, ALL_ACCESS_TYPES);
		
		assertEquals(new HashSet(Arrays.asList(new String[]{ALL_ACCESS_TYPES})), permitted);
	}

	@Test
	public void testGetPermittedAccessTypesRegistryNoViewScope() throws Exception {
		// method under test:
		Set<String> permitted = authorizationManager.
				getPermittedDockerActions(ADMIN_INFO, Collections.EMPTY_LIST, SERVICE, REGISTRY_TYPE, CATALOG_NAME, ALL_ACCESS_TYPES);
		
		assertTrue(permitted.isEmpty());
	}

	@Test
	public void testGetPermittedAccessTypesRegistryNotAdmin() throws Exception {
		// method under test:
		Set<String> permitted = authorizationManager.
				getPermittedDockerActions(USER_INFO, OAUTH_SCOPES, SERVICE, REGISTRY_TYPE, CATALOG_NAME, ALL_ACCESS_TYPES);
		
		assertTrue(permitted.isEmpty());
	}

	@Test
	public void testGetPermittedAccessTypesRegistryNotCatalog() throws Exception {
		// method under test:
		Set<String> permitted = authorizationManager.
				getPermittedDockerActions(ADMIN_INFO, OAUTH_SCOPES, SERVICE, REGISTRY_TYPE, "not-catalog", ALL_ACCESS_TYPES);
		
		assertTrue(permitted.isEmpty());
	}

	@Test
	public void testGetPermittedAccessTypesInvalidParent() throws Exception {
		String repositoryPath = "garbage/"+REPOSITORY_NAME;
		
		// method under test:
		Set<String> permitted = authorizationManager.
				getPermittedDockerActions(USER_INFO, OAUTH_SCOPES, SERVICE, REPOSITORY_TYPE, repositoryPath, ACCESS_TYPES_STRING);
		
		assertTrue(permitted.isEmpty());
	}

	@Test
	public void testGetPermittedAccessTypesNonexistentChild() throws Exception {
		String repositoryPath = PARENT_ID+"/non-existent-repo";

		when(mockEntityAuthorizationManager.canCreate(eq(PARENT_ID), eq(EntityType.dockerrepo), eq(USER_INFO))).
			thenReturn(AuthorizationStatus.authorized());
	
		when(mockDockerNodeDao.getEntityIdForRepositoryName(SERVICE+"/"+repositoryPath)).thenReturn(null);

		// method under test:
		Set<String> permitted = authorizationManager.
				getPermittedDockerActions(USER_INFO, OAUTH_SCOPES, SERVICE, REPOSITORY_TYPE, repositoryPath, ACCESS_TYPES_STRING);
		
		// client needs both push and pull access to push a not-yet-existing repo to the registry
		assertEquals(new HashSet(Arrays.asList(new String[]{push.name(), pull.name()})), permitted);
	}

	@Test
	public void testGetPermittedAccessRepoExistsAccessUnauthorized() throws Exception {
		when(mockEntityAuthorizationManager.hasAccess(eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ACCESS_TYPE.UPDATE))).
			thenReturn(AuthorizationStatus.accessDenied(""));

		when(mockEntityAuthorizationManager.hasAccess(eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ACCESS_TYPE.DOWNLOAD))).
			thenReturn(AuthorizationStatus.accessDenied(""));
		
		// method under test:
		Set<String> permitted = authorizationManager.
				getPermittedDockerActions(USER_INFO, OAUTH_SCOPES, SERVICE, REPOSITORY_TYPE, REPOSITORY_PATH, ACCESS_TYPES_STRING);

		// Note, we DO have create access, but that doesn't let us 'push' since the repo already exists
		assertTrue(permitted.isEmpty(), permitted.toString());
	}

	@Test
	public void testGetPermittedAccessRepoExistsAccessUnauthorizedBUTWasSubmitted() throws Exception {
		when(mockEntityAuthorizationManager.hasAccess(eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ACCESS_TYPE.UPDATE))).
			thenReturn(AuthorizationStatus.accessDenied(""));

		when(mockEntityAuthorizationManager.hasAccess(eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ACCESS_TYPE.DOWNLOAD))).
			thenReturn(AuthorizationStatus.accessDenied(""));

		when(mockEvaluationPermissionsManager.
				isDockerRepoNameInEvaluationWithAccess(REPOSITORY_NAME, 
						USER_INFO.getGroups(), 
						ACCESS_TYPE.READ_PRIVATE_SUBMISSION)).thenReturn(true);

		// method under test:
		Set<String> permitted = authorizationManager.
				getPermittedDockerActions(USER_INFO, OAUTH_SCOPES, SERVICE, REPOSITORY_TYPE, REPOSITORY_PATH, ACCESS_TYPES_STRING);

		// Note, we can pull (but not push!) since we have admin access to evaluation
		assertEquals(new HashSet(Arrays.asList(new String[]{pull.name()})), permitted);
	}

	@Test
	public void testGetPermittedAccessDownloadButNoRead() throws Exception {
		when(mockEntityAuthorizationManager.hasAccess(eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ACCESS_TYPE.UPDATE))).
			thenReturn(AuthorizationStatus.accessDenied(""));

		when(mockEntityAuthorizationManager.hasAccess(eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ACCESS_TYPE.DOWNLOAD))).
			thenReturn(AuthorizationStatus.authorized());
		
		// method under test:
		Set<String> permitted = authorizationManager.
				getPermittedDockerActions(USER_INFO, OAUTH_SCOPES, SERVICE, REPOSITORY_TYPE, REPOSITORY_PATH, "pull");

		// it's allowed because it's *DOWNLOAD* permission, not *READ* permission which we must have
		assertEquals(new HashSet(Arrays.asList(new String[]{pull.name()})), permitted);

	}

	@Test
	public void testGetPermittedAccessTypesNonexistentChildUnauthorized() throws Exception {
		String repositoryPath = PARENT_ID+"/non-existent-repo";

		when(mockDockerNodeDao.getEntityIdForRepositoryName(SERVICE+"/"+repositoryPath)).thenReturn(null);

		when(mockEntityAuthorizationManager.canCreate(eq(PARENT_ID), eq(EntityType.dockerrepo), eq(USER_INFO))).
			thenReturn(AuthorizationStatus.accessDenied(""));
		
		when(mockEntityAuthorizationManager.hasAccess(eq(USER_INFO), eq(REPO_ENTITY_ID), eq(ACCESS_TYPE.DOWNLOAD))).
			thenReturn(AuthorizationStatus.accessDenied(""));
		
		// method under test:
		Set<String> permitted = authorizationManager.
				getPermittedDockerActions(USER_INFO, OAUTH_SCOPES, SERVICE, REPOSITORY_TYPE, repositoryPath, ACCESS_TYPES_STRING);

		// Note, we DO have update access, but that doesn't let us 'push' since the repo doesn't exist
		assertTrue(permitted.isEmpty(), permitted.toString());
	}
	
	@Test
	public void testGetPermittedAccessTypesRepoInTrash() throws Exception {
		when(mockNodeDao.getBenefactor(REPO_ENTITY_ID)).thenReturn(KeyFactory.keyToString(AuthorizationManagerImpl.TRASH_FOLDER_ID));

		// method under test:
		Set<String> permitted = authorizationManager.
				getPermittedDockerActions(USER_INFO, OAUTH_SCOPES, SERVICE, REPOSITORY_TYPE, REPOSITORY_PATH, ACCESS_TYPES_STRING);
		
		assertTrue(permitted.isEmpty(), permitted.toString());
	}


	@Test
	public void testGetPermittedAccessRepoExistsInTrashBUTWasSubmitted() throws Exception {
		when(mockNodeDao.getBenefactor(REPO_ENTITY_ID)).thenReturn(KeyFactory.keyToString(AuthorizationManagerImpl.TRASH_FOLDER_ID));

		when(mockEvaluationPermissionsManager.
				isDockerRepoNameInEvaluationWithAccess(REPOSITORY_NAME, 
						USER_INFO.getGroups(), 
						ACCESS_TYPE.READ_PRIVATE_SUBMISSION)).thenReturn(true);

		// method under test:
		Set<String> permitted = authorizationManager.
				getPermittedDockerActions(USER_INFO, OAUTH_SCOPES, SERVICE, REPOSITORY_TYPE, REPOSITORY_PATH, ACCESS_TYPES_STRING);

		// Note, we can pull (but not push!) since we have admin access to evaluation
		assertEquals(Collections.singleton(pull.name()), permitted);
	}

	@Test
	public void testValidateWithCertifiedUserRequiredNotSatisfied() {
		req.setIsCertifiedUserRequired(true);
		req.setIsValidatedProfileRequired(false);
		when(mockGroupMembersDao.areMemberOf(
				AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString(),
				accessors))
				.thenReturn(false);
		assertThrows(UserCertificationRequiredException.class, ()-> {
			authorizationManager.validateHasAccessorRequirement(req, accessors);
		});
		verifyZeroInteractions(mockVerificationDao);
	}

	@Test
	public void testValidateWithValidatedProfileRequiredNotSatisfied() {
		req.setIsCertifiedUserRequired(false);
		req.setIsValidatedProfileRequired(true);
		when(mockVerificationDao.haveValidatedProfiles(accessors)).thenReturn(false);
		assertThrows(IllegalArgumentException.class, ()-> {
			authorizationManager.validateHasAccessorRequirement(req, accessors);
		});
		verifyZeroInteractions(mockGroupMembersDao);
	}

	@Test
	public void testValidateWithCertifiedUserRequiredAndValidatedProfileSatisfied() {
		req.setIsCertifiedUserRequired(true);
		req.setIsValidatedProfileRequired(true);
		when(mockGroupMembersDao.areMemberOf(
				AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString(),
				accessors))
				.thenReturn(true);
		when(mockVerificationDao.haveValidatedProfiles(accessors)).thenReturn(true);
		authorizationManager.validateHasAccessorRequirement(req, accessors);
	}

	@Test
	public void testValidateWithoutRequirements() {
		req.setIsCertifiedUserRequired(false);
		req.setIsValidatedProfileRequired(false);
		authorizationManager.validateHasAccessorRequirement(req, accessors);
		verifyZeroInteractions(mockGroupMembersDao);
		verifyZeroInteractions(mockVerificationDao);
	}
}
