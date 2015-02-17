package org.sagebionetworks.evaluation.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.evaluation.model.BatchUploadResponse;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.EvaluationSubmissions;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionContributor;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusBatch;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.evaluation.EvaluationSubmissionsDAO;
import org.sagebionetworks.repo.model.evaluation.SubmissionDAO;
import org.sagebionetworks.repo.model.evaluation.SubmissionFileHandleDAO;
import org.sagebionetworks.repo.model.evaluation.SubmissionStatusDAO;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.test.util.ReflectionTestUtils;

public class SubmissionManagerTest {
		
	private SubmissionManager submissionManager;	
	private Evaluation eval;
	private Submission sub;
	private Submission sub2;
	private Submission subWithId;
	private Submission sub2WithId;
	private SubmissionStatus subStatus;
	private SubmissionStatusBatch batch;
	
	private IdGenerator mockIdGenerator;
	private SubmissionDAO mockSubmissionDAO;
	private SubmissionStatusDAO mockSubmissionStatusDAO;
	private SubmissionFileHandleDAO mockSubmissionFileHandleDAO;
	private EvaluationSubmissionsDAO mockEvaluationSubmissionsDAO;
	private EntityManager mockEntityManager;
	private NodeManager mockNodeManager;
	private FileHandleManager mockFileHandleManager;
	private EvaluationPermissionsManager mockEvalPermissionsManager;
	private SubmissionEligibilityManager mockSubmissionEligibilityManager;
	private Node mockNode;
	private Folder folder;
	private EntityBundle bundle;
	
    private FileHandle fileHandle1;
    private FileHandle fileHandle2;
	
	private static final String EVAL_ID = "12";
	private static final Long EVAL_ID_LONG = Long.parseLong("12");
	private static final String OWNER_ID = "34";
	private static final String USER_ID = "56";
	private static final String SUB_ID = "78";
	private static final String SUB2_ID = "87";
	private static final String ENTITY_ID = "90";
	private static final String ENTITY2_ID = "99";
	private static final String ETAG = "etag";	
	private static final String HANDLE_ID_2 = "handle2";
	private static final String HANDLE_ID_1 = "handle1";
	private static final String TEST_URL = "http://www.foo.com/bar";
	private static final String TEAM_ID = "999";
	
	private static UserInfo ownerInfo;
	private static UserInfo userInfo;
	
    @Before
    public void setUp() throws DatastoreException, NotFoundException, InvalidModelException, MalformedURLException {
		// User Info
    	ownerInfo = new UserInfo(false, OWNER_ID);
    	userInfo = new UserInfo(false, USER_ID);
    	
    	// FileHandles
		List<FileHandle> handles = new ArrayList<FileHandle>();
		List<String> handleIds = new ArrayList<String>();
		fileHandle1 = new PreviewFileHandle();
		fileHandle1.setId(HANDLE_ID_1);
		handles.add(fileHandle1);
		handleIds.add(HANDLE_ID_1);
		fileHandle2 = new PreviewFileHandle();
		fileHandle2.setId(HANDLE_ID_2);
		handles.add(fileHandle2);
		handleIds.add(HANDLE_ID_2);
    	
    	// Objects
		eval = new Evaluation();
		eval.setName("compName");
		eval.setId(EVAL_ID);
		eval.setOwnerId(OWNER_ID);
        eval.setContentSource(KeyFactory.SYN_ROOT_ID);
        eval.setStatus(EvaluationStatus.CLOSED); // deprecated, setting doesn't matter
        eval.setCreatedOn(new Date());
        eval.setEtag("compEtag");
        
        sub = new Submission();
        sub.setEvaluationId(EVAL_ID);
        sub.setCreatedOn(new Date());
        sub.setEntityId(ENTITY_ID);
        sub.setName("subName");
        sub.setUserId(USER_ID);
        sub.setSubmitterAlias("Team Awesome!");
        sub.setVersionNumber(0L);
        
        sub2 = new Submission();
        sub2.setEvaluationId(EVAL_ID);
        sub2.setCreatedOn(new Date());
        sub2.setEntityId(ENTITY2_ID);
        sub2.setName("subName");
        sub2.setUserId(OWNER_ID);
        sub2.setSubmitterAlias("Team Even More Awesome!");
        sub2.setVersionNumber(0L);
        
        subWithId = new Submission();
        subWithId.setId(SUB_ID);
        subWithId.setEvaluationId(EVAL_ID);
        subWithId.setCreatedOn(new Date());
        subWithId.setEntityId(ENTITY_ID);
        subWithId.setName("subName");
        subWithId.setUserId(USER_ID);
        subWithId.setSubmitterAlias("Team Awesome!");
        subWithId.setVersionNumber(0L);
        
        sub2WithId = new Submission();
        sub2WithId.setId(SUB2_ID);
        sub2WithId.setEvaluationId(EVAL_ID);
        sub2WithId.setCreatedOn(new Date());
        sub2WithId.setEntityId(ENTITY2_ID);
        sub2WithId.setName("subName");
        sub2WithId.setUserId(OWNER_ID);
        sub2WithId.setSubmitterAlias("Team Even More Awesome!");
        sub2WithId.setVersionNumber(0L);
        
        subStatus = new SubmissionStatus();
        subStatus.setEtag("subEtag");
        subStatus.setId(SUB_ID);
        subStatus.setModifiedOn(new Date());
        subStatus.setScore(0.0);
        subStatus.setStatus(SubmissionStatusEnum.RECEIVED);
        subStatus.setAnnotations(createDummyAnnotations());
        
        batch = new SubmissionStatusBatch();
        List<SubmissionStatus> statuses = new ArrayList<SubmissionStatus>();
        statuses.add(subStatus);
        batch.setStatuses(statuses);
        batch.setIsFirstBatch(true);
        batch.setIsLastBatch(true);
        
        folder = new Folder();
        bundle = new EntityBundle();
        bundle.setEntity(folder);
        bundle.setFileHandles(handles);
		
    	// Mocks
        mockIdGenerator = mock(IdGenerator.class);
    	mockSubmissionDAO = mock(SubmissionDAO.class);
    	mockSubmissionStatusDAO = mock(SubmissionStatusDAO.class);
    	mockSubmissionFileHandleDAO = mock(SubmissionFileHandleDAO.class);
    	mockEvaluationSubmissionsDAO = mock(EvaluationSubmissionsDAO.class);
    	mockEntityManager = mock(EntityManager.class);
    	mockNodeManager = mock(NodeManager.class, RETURNS_DEEP_STUBS);
    	mockNode = mock(Node.class);
      	mockFileHandleManager = mock(FileHandleManager.class);
      	mockEvalPermissionsManager = mock(EvaluationPermissionsManager.class);
      	mockSubmissionEligibilityManager = mock(SubmissionEligibilityManager.class);

    	when(mockIdGenerator.generateNewId()).thenReturn(Long.parseLong(SUB_ID));
    	when(mockSubmissionDAO.get(eq(SUB_ID))).thenReturn(subWithId);
    	when(mockSubmissionDAO.get(eq(SUB2_ID))).thenReturn(sub2WithId);
    	when(mockSubmissionDAO.create(eq(sub))).thenReturn(SUB_ID);
    	when(mockSubmissionStatusDAO.get(eq(SUB_ID))).thenReturn(subStatus);
    	when(mockNode.getNodeType()).thenReturn(EntityType.values()[0].toString());
    	when(mockNode.getETag()).thenReturn(ETAG);
    	when(mockNodeManager.get(any(UserInfo.class), eq(ENTITY_ID))).thenReturn(mockNode);    	
    	when(mockNodeManager.get(eq(userInfo), eq(ENTITY2_ID))).thenThrow(new UnauthorizedException());
    	when(mockNodeManager.getNodeForVersionNumber(eq(userInfo), eq(ENTITY_ID), anyLong())).thenReturn(mockNode);
    	when(mockNodeManager.getNodeForVersionNumber(eq(userInfo), eq(ENTITY2_ID), anyLong())).thenThrow(new UnauthorizedException());
    	when(mockEntityManager.getEntityForVersion(any(UserInfo.class), anyString(), anyLong(), any(Class.class))).thenReturn(folder);
    	when(mockSubmissionFileHandleDAO.getAllBySubmission(eq(SUB_ID))).thenReturn(handleIds);
		when(mockFileHandleManager.getRedirectURLForFileHandle(eq(HANDLE_ID_1))).thenReturn(TEST_URL);
    	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.SUBMIT))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
       	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.READ))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
       	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.READ_PRIVATE_SUBMISSION))).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
       	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.UPDATE_SUBMISSION))).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
       	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.DELETE_SUBMISSION))).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
    	when(mockEvalPermissionsManager.hasAccess(eq(ownerInfo), eq(EVAL_ID), any(ACCESS_TYPE.class))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
    	when(mockSubmissionStatusDAO.getEvaluationIdForBatch((List<SubmissionStatus>)anyObject())).thenReturn(Long.parseLong(EVAL_ID));

    	// by default we say that individual submissions are within quota
    	// (specific tests will change this)
    	when(mockSubmissionEligibilityManager.isIndividualEligible(eq(EVAL_ID), any(UserInfo.class), any(Date.class))).
    		thenReturn(AuthorizationManagerUtil.AUTHORIZED);
    	
    	// Submission Manager
    	submissionManager = new SubmissionManagerImpl();
    	ReflectionTestUtils.setField(submissionManager, "idGenerator", mockIdGenerator);
    	ReflectionTestUtils.setField(submissionManager, "submissionDAO", mockSubmissionDAO);
    	ReflectionTestUtils.setField(submissionManager, "submissionStatusDAO", mockSubmissionStatusDAO);
    	ReflectionTestUtils.setField(submissionManager, "submissionFileHandleDAO", mockSubmissionFileHandleDAO);
    	ReflectionTestUtils.setField(submissionManager, "evaluationSubmissionsDAO", mockEvaluationSubmissionsDAO);
    	ReflectionTestUtils.setField(submissionManager, "entityManager", mockEntityManager);
    	ReflectionTestUtils.setField(submissionManager, "nodeManager", mockNodeManager);
    	ReflectionTestUtils.setField(submissionManager, "fileHandleManager", mockFileHandleManager);
    	ReflectionTestUtils.setField(submissionManager, "evaluationPermissionsManager", mockEvalPermissionsManager);
    	ReflectionTestUtils.setField(submissionManager, "submissionEligibilityManager", mockSubmissionEligibilityManager);
    }
	
	@Test
	public void testCRUDAsAdmin() throws Exception {
		assertNull(sub.getId());
		assertNotNull(subWithId.getId());
		submissionManager.createSubmission(userInfo, sub, ETAG, null, bundle);
		submissionManager.getSubmission(ownerInfo, SUB_ID);
		submissionManager.updateSubmissionStatus(ownerInfo, subStatus);
		submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
		submissionManager.deleteSubmission(ownerInfo, SUB_ID);
		verify(mockSubmissionDAO).create(any(Submission.class));
		verify(mockSubmissionDAO).delete(eq(SUB_ID));
		verify(mockSubmissionStatusDAO).create(any(SubmissionStatus.class));
		verify(mockSubmissionStatusDAO, times(2)).update(any(List.class));
		verify(mockSubmissionFileHandleDAO).create(eq(SUB_ID), eq(fileHandle1.getId()));
		verify(mockSubmissionFileHandleDAO).create(eq(SUB_ID), eq(fileHandle2.getId()));
		verify(mockEvaluationSubmissionsDAO, times(1)).lockAndGetForEvaluation(EVAL_ID_LONG);
		verify(mockEvaluationSubmissionsDAO, times(1)).updateEtagForEvaluation(EVAL_ID_LONG, true, ChangeType.CREATE);
		verify(mockEvaluationSubmissionsDAO, times(3)).updateEtagForEvaluation(EVAL_ID_LONG, true, ChangeType.UPDATE);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testCAsUser_NotAuthorized() throws Exception {
		assertNull(sub.getId());
		assertNotNull(subWithId.getId());
		when(mockEvalPermissionsManager.hasAccess(
				eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.SUBMIT))).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		submissionManager.createSubmission(userInfo, sub, ETAG, null, bundle);		
	}
	
	@Test
	public void testCRUDAsUser() throws NotFoundException, DatastoreException, JSONObjectAdapterException {
		assertNull(sub.getId());
		assertNotNull(subWithId.getId());
		submissionManager.createSubmission(userInfo, sub, ETAG, null, bundle);
		try {
			submissionManager.getSubmission(userInfo, SUB_ID);
			fail();
		} catch (UnauthorizedException e) {
			// expected
		}
		try {
			submissionManager.updateSubmissionStatus(userInfo, subStatus);
			fail();
		} catch (UnauthorizedException e) {
			//expected
		}
		try {
			submissionManager.updateSubmissionStatusBatch(userInfo, EVAL_ID, batch);
			fail();
		} catch (UnauthorizedException e) {
			//expected
		}
		try {
			submissionManager.deleteSubmission(userInfo, SUB_ID);
			fail();
		} catch (UnauthorizedException e) {
			//expected
		}
		verify(mockSubmissionDAO).create(any(Submission.class));
		verify(mockSubmissionDAO, never()).delete(eq(SUB_ID));
		verify(mockSubmissionStatusDAO).create(any(SubmissionStatus.class));
		verify(mockSubmissionStatusDAO, never()).update(any(List.class));
	}
	
	@Test
	public void testInsertUserAsContributor_NullContributorList() throws Exception {
		assertNull(sub.getContributors());
		submissionManager.createSubmission(userInfo, sub, ETAG, null, bundle);
		assertEquals(1, sub.getContributors().size());
		SubmissionContributor sc =sub.getContributors().iterator().next();
		assertNotNull(sc.getCreatedOn());
		assertEquals(USER_ID, sc.getPrincipalId());
		verify(mockSubmissionEligibilityManager).
			isIndividualEligible(eq(EVAL_ID), any(UserInfo.class), any(Date.class));
		verify(mockSubmissionEligibilityManager, never()).
			isTeamEligible(anyString(), anyString(),any(List.class), anyString(), any(Date.class));
	}
	
	@Test
	public void testInsertUserAsContributor_SubmitterIsContributor() throws Exception {
		Set<SubmissionContributor> contributors = new HashSet<SubmissionContributor>();
		SubmissionContributor sc = new SubmissionContributor();
		sc.setPrincipalId(USER_ID);
		contributors.add(sc);
		sub.setContributors(contributors);
		submissionManager.createSubmission(userInfo, sub, ETAG, null, bundle);
		assertEquals(1, sub.getContributors().size());
		sc =sub.getContributors().iterator().next();
		assertNotNull(sc.getCreatedOn());
		assertEquals(USER_ID, sc.getPrincipalId());
		verify(mockSubmissionEligibilityManager).
			isIndividualEligible(eq(EVAL_ID), any(UserInfo.class), any(Date.class));
		verify(mockSubmissionEligibilityManager, never()).
			isTeamEligible(anyString(), anyString(),any(List.class), anyString(), any(Date.class));
	}
	
	@Test
	public void testTeamSubmission() throws Exception {
		assertNull(sub.getContributors());
		Set<SubmissionContributor> contributors = new HashSet<SubmissionContributor>();
		SubmissionContributor sc = new SubmissionContributor();
		sc.setPrincipalId(OWNER_ID);
		contributors.add(sc);
		sub.setContributors(contributors);
		sub.setTeamId(TEAM_ID);
		int submissionEligibilityHash = 1234;
		when(mockSubmissionEligibilityManager.isTeamEligible(
				eq(EVAL_ID), eq(TEAM_ID),
				any(List.class), eq(""+submissionEligibilityHash), any(Date.class))).
				thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		submissionManager.createSubmission(userInfo, sub, ETAG, ""+submissionEligibilityHash, bundle);
		assertEquals(2, sub.getContributors().size());
		boolean foundUser = false;
		boolean foundOwner = false;
		assertEquals(2, sub.getContributors().size());
		for (SubmissionContributor sc2 : sub.getContributors()) {
			assertNotNull(sc2.getCreatedOn());
			if (USER_ID.equals(sc2.getPrincipalId())) foundUser=true;
			if (OWNER_ID.equals(sc2.getPrincipalId())) foundOwner=true;
		}
		assertTrue(foundUser);
		assertTrue(foundOwner);
		verify(mockSubmissionEligibilityManager, never()).
				isIndividualEligible(eq(EVAL_ID), any(UserInfo.class), any(Date.class));
		verify(mockSubmissionEligibilityManager).
			isTeamEligible(eq(EVAL_ID), eq(TEAM_ID),
					any(List.class), eq(""+submissionEligibilityHash), any(Date.class));
	}
	
	@Test(expected = InvalidModelException.class)
	public void testContributorListButNoTeamId() throws Exception {
		assertNull(sub.getContributors());
		Set<SubmissionContributor> contributors = new HashSet<SubmissionContributor>();
		SubmissionContributor sc = new SubmissionContributor();
		sc.setPrincipalId(OWNER_ID);
		contributors.add(sc);
		sub.setContributors(contributors);
		submissionManager.createSubmission(userInfo, sub, ETAG, null, bundle);
	}
	
	@Test(expected = InvalidModelException.class)
	public void testIndividualSubmissionWithHash() throws Exception {
		assertNull(sub.getContributors());
		submissionManager.createSubmission(userInfo, sub, ETAG, "123456", bundle);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testUnauthorizedGet() throws NotFoundException, DatastoreException, JSONObjectAdapterException {
		submissionManager.getSubmission(userInfo, SUB2_ID);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testUnauthorizedEntity() throws NotFoundException, DatastoreException, JSONObjectAdapterException {		
		// user should not have access to sub2
		submissionManager.createSubmission(userInfo, sub2, ETAG, null, bundle);		
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidScore() throws Exception {
		submissionManager.createSubmission(userInfo, sub, ETAG, null, bundle);
		submissionManager.getSubmission(ownerInfo, SUB_ID);
		subStatus.setScore(1.1);
		submissionManager.updateSubmissionStatus(ownerInfo, subStatus);
	}
		
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidEtag() throws Exception {
		submissionManager.createSubmission(userInfo, sub, ETAG + "modified", null, bundle);
	}
	
	@Test
	public void testGetAllSubmissions() throws DatastoreException, UnauthorizedException, NotFoundException {
		SubmissionStatusEnum statusEnum = SubmissionStatusEnum.SCORED;
		submissionManager.getAllSubmissions(ownerInfo, EVAL_ID, null, 10, 0);
		submissionManager.getAllSubmissions(ownerInfo, EVAL_ID, statusEnum, 10, 0);
		verify(mockSubmissionDAO).getAllByEvaluation(eq(EVAL_ID), anyLong(), anyLong());
		verify(mockSubmissionDAO).getAllByEvaluationAndStatus(eq(EVAL_ID), eq(statusEnum), eq(10L), eq(0L));
	}
	
	@Test(expected = UnauthorizedException.class)
	public void testGetAllSubmissionsUnauthorized() throws DatastoreException, UnauthorizedException, NotFoundException {
    	when(mockEvalPermissionsManager.hasAccess(eq(ownerInfo), eq(USER_ID), any(ACCESS_TYPE.class))).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		submissionManager.getAllSubmissions(ownerInfo, USER_ID, null, 10, 0);
	}
	
	@Test
	public void testGetMyOwnSubmissionBundles() throws Exception {
		submissionManager.getMyOwnSubmissionBundlesByEvaluation(ownerInfo, EVAL_ID, 10, 0);
		verify(mockSubmissionDAO).getAllByEvaluationAndUser(EVAL_ID, ownerInfo.getId().toString(), 10, 0);
		verify(mockSubmissionDAO).getCountByEvaluationAndUser(EVAL_ID, ownerInfo.getId().toString());
	}
	
	@Test
	public void testGetMyOwnSubmissions() throws Exception {
		submissionManager.getMyOwnSubmissionsByEvaluation(ownerInfo, EVAL_ID, 10, 0);
		verify(mockSubmissionDAO).getAllByEvaluationAndUser(EVAL_ID, ownerInfo.getId().toString(), 10, 0);
		verify(mockSubmissionDAO).getCountByEvaluationAndUser(EVAL_ID, ownerInfo.getId().toString());
	}
	
	@Test
	public void testGetSubmissionCount() throws DatastoreException, NotFoundException {
		submissionManager.getSubmissionCount(ownerInfo, EVAL_ID);
		verify(mockSubmissionDAO).getCountByEvaluation(eq(EVAL_ID));
	}
	
	@Test
	public void testGetRedirectURLForFileHandle()
			throws DatastoreException, NotFoundException {
		String url = submissionManager.getRedirectURLForFileHandle(ownerInfo, SUB_ID, HANDLE_ID_1);
		assertEquals(TEST_URL, url);
		verify(mockSubmissionFileHandleDAO).getAllBySubmission(eq(SUB_ID));
		verify(mockFileHandleManager).getRedirectURLForFileHandle(eq(HANDLE_ID_1));
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testGetRedirectURLForFileHandleUnauthorized()
			throws DatastoreException, NotFoundException {
		submissionManager.getRedirectURLForFileHandle(userInfo, SUB_ID, HANDLE_ID_1);
	}
	
	@Test(expected=NotFoundException.class)
	public void testGetRedirectURLForFileHandleNotFound() 
			throws DatastoreException, NotFoundException {
		// HANDLE_ID_1 is not contained in this Submission
		submissionManager.getRedirectURLForFileHandle(ownerInfo, SUB2_ID, HANDLE_ID_1);
	}
	
	@Test
	public void testGetSubmissionStatus() throws DatastoreException, NotFoundException {
		SubmissionStatus status = submissionManager.getSubmissionStatus(ownerInfo, SUB_ID);
		Annotations annos = status.getAnnotations();
		assertNotNull(annos);
		
		// check the single private LongAnno
		assertNotNull(annos.getLongAnnos());		
		assertEquals(1, annos.getLongAnnos().size());		
		LongAnnotation la = annos.getLongAnnos().get(0);
		assertTrue(la.getIsPrivate());
		
		// check the single StringAnno
		assertNotNull(annos.getStringAnnos());
		assertEquals(1, annos.getStringAnnos().size());		

		// check the single DoubleAnno
		assertNotNull(annos.getDoubleAnnos());
		assertEquals(1, annos.getDoubleAnnos().size());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testGetSubmissionStatusNoREADAccess() throws Exception {
    	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.READ))).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		submissionManager.getSubmissionStatus(userInfo, SUB_ID);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testGetALLSubmissionStatusNoREADAccess() throws Exception {
    	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.READ))).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		submissionManager.getAllSubmissionStatuses(userInfo, EVAL_ID, null, 100L, 0L);
	}
	
	@Test
	public void testGetSubmissionStatusNoPrivate() throws DatastoreException, NotFoundException {
		SubmissionStatus status = submissionManager.getSubmissionStatus(userInfo, SUB_ID);
		Annotations annos = status.getAnnotations();
		assertNotNull(annos);
		
		// check that the single private LongAnno was removed
		assertNotNull(annos.getLongAnnos());		
		assertEquals(0, annos.getLongAnnos().size());
		
		// check the single StringAnno
		assertNotNull(annos.getStringAnnos());
		assertEquals(1, annos.getStringAnnos().size());		

		// check the single DoubleAnno
		assertNotNull(annos.getDoubleAnnos());
		assertEquals(1, annos.getDoubleAnnos().size());
	}
	
	@Test
	public void testGetSubmissionStatusNoPrivateNoAnnot() throws DatastoreException, NotFoundException {
 		Annotations annots = subStatus.getAnnotations();
		annots.setStringAnnos(null); // this is the case in PLFM-2586
		annots.setLongAnnos(null);
		annots.setDoubleAnnos(null);
		submissionManager.getSubmissionStatus(userInfo, SUB_ID);
	}
	
	private static Annotations createDummyAnnotations() {		
		List<StringAnnotation> stringAnnos = new ArrayList<StringAnnotation>();
		StringAnnotation sa = new StringAnnotation();
		sa.setIsPrivate(false);
		sa.setKey("sa");
		sa.setValue("foo");
		stringAnnos.add(sa);
		
		List<LongAnnotation> longAnnos = new ArrayList<LongAnnotation>();
		LongAnnotation la = new LongAnnotation();
		la.setIsPrivate(true);
		la.setKey("la");
		la.setValue(42L);
		longAnnos.add(la);
		
		List<DoubleAnnotation> doubleAnnos = new ArrayList<DoubleAnnotation>();
		DoubleAnnotation doa = new DoubleAnnotation();
		doa.setIsPrivate(false);
		doa.setKey("doa");
		doa.setValue(3.14);
		doubleAnnos.add(doa);
		
		Annotations annos = new Annotations();
		annos.setStringAnnos(stringAnnos);
		annos.setLongAnnos(longAnnos);
		annos.setDoubleAnnos(doubleAnnos);
		return annos;
	}

	@Test
	public void testBadBatch() throws Exception {
		// baseline:  all is good
		submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
		
		batch.setIsFirstBatch(null);
		try {
			submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
			fail("firstBatch can't be null");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		batch.setIsFirstBatch(true);
		submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
		
		batch.setIsLastBatch(null);
		try {
			submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
			fail("lastBatch can't be null");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		batch.setIsLastBatch(true);
		submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
		
		List<SubmissionStatus> statuses = new ArrayList<SubmissionStatus>(batch.getStatuses());
		batch.getStatuses().clear();
		try {
			submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
			fail("statuses can't be empty");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		batch.setStatuses(null);
		try {
			submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
			fail("statuses can't be null");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		List<SubmissionStatus> tooLong = new ArrayList<SubmissionStatus>();
		for (int i=0; i<501; i++) tooLong.add(new SubmissionStatus());
		batch.setStatuses(tooLong);
		try {
			submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
			fail("statuses can't be this many");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		List<SubmissionStatus> hasNull = new ArrayList<SubmissionStatus>();
		hasNull.add(null);
		batch.setStatuses(hasNull);
		try {
			submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
			fail("statuses can't pass null");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		
		// back to base-line
		batch.setStatuses(statuses);
		submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
		
		try {
			submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID+"xxx", batch);
			fail("statuses can't wrong ID");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}
	
	@Test
	public void testConflictingBatchUpdate() throws Exception {
		// baseline:  all is OK
		submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
		batch.setIsFirstBatch(false);
		batch.setBatchToken("foo");
		EvaluationSubmissions evalSubs = new EvaluationSubmissions();
		evalSubs.setEtag("foo");
		when(mockEvaluationSubmissionsDAO.lockAndGetForEvaluation(EVAL_ID_LONG)).thenReturn(evalSubs);
		// still OK
		submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
		
		// but what if the token is wrong?
		evalSubs.setEtag("bar");
		when(mockEvaluationSubmissionsDAO.lockAndGetForEvaluation(EVAL_ID_LONG)).thenReturn(evalSubs);
		try {
			submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
			fail("ConflictingUpdateException expected");
		} catch (ConflictingUpdateException e) {
			// as expected
		}
	}
	
	@Test
	public void testBatchResponseToken() throws Exception {
		batch.setIsFirstBatch(true);
		batch.setIsLastBatch(false);
		when(mockEvaluationSubmissionsDAO.updateEtagForEvaluation(EVAL_ID_LONG, false, ChangeType.UPDATE)).thenReturn("foo");
		BatchUploadResponse resp = submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
		verify(mockEvaluationSubmissionsDAO).updateEtagForEvaluation(EVAL_ID_LONG, false, ChangeType.UPDATE);
		assertEquals(resp.getNextUploadToken(), "foo");
		
		// last batch doesn't get a token back
		batch.setIsLastBatch(true);
		resp = submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
		assertNull(resp.getNextUploadToken());
	}
}
