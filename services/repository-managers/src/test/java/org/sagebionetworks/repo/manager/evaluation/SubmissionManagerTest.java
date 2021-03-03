package org.sagebionetworks.repo.manager.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.EvaluationSubmissionsDAO;
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.evaluation.dao.SubmissionFileHandleDAO;
import org.sagebionetworks.evaluation.dao.SubmissionStatusDAO;
import org.sagebionetworks.evaluation.model.BatchUploadResponse;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationSubmissions;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionBundle;
import org.sagebionetworks.evaluation.model.SubmissionContributor;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusBatch;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.evaluation.EvaluationPermissionsManager;
import org.sagebionetworks.repo.manager.evaluation.SubmissionEligibilityManager;
import org.sagebionetworks.repo.manager.evaluation.SubmissionManagerImpl;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.FileHandleUrlRequest;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DockerCommitDao;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.annotation.AnnotationBase;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.MessageToSend;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.util.Pair;

import com.google.common.collect.ImmutableSet;

@ExtendWith(MockitoExtension.class)
public class SubmissionManagerTest {
		
	private Evaluation eval;
	private Submission sub;
	private Submission sub2;
	private Submission subWithId;
	private Submission sub2WithId;
	private SubmissionStatus subStatus;
	private SubmissionBundle submissionBundle;
	private EntityBundle dockerBundle;

	private SubmissionStatusBatch batch;
	
	@Mock
	private IdGenerator mockIdGenerator;
	@Mock
	private SubmissionDAO mockSubmissionDAO;
	@Mock
	private SubmissionStatusDAO mockSubmissionStatusDAO;
	@Mock
	private SubmissionFileHandleDAO mockSubmissionFileHandleDAO;
	@Mock
	private EvaluationSubmissionsDAO mockEvaluationSubmissionsDAO;
	@Mock
	private EntityManager mockEntityManager;
	@Mock
	private NodeManager mockNodeManager;
	@Mock
	private FileHandleManager mockFileHandleManager;
	@Mock
	private EvaluationPermissionsManager mockEvalPermissionsManager;
	@Mock
	private SubmissionEligibilityManager mockSubmissionEligibilityManager;
	@Mock
	private Node mockNode;
	@Mock
	private Node mockDockerRepoNode;
	@Mock
	private TeamDAO mockTeamDAO;
	@Mock
	private UserProfileManager mockUserProfileManager;
	@Mock
	private EvaluationDAO mockEvaluationDAO;
	@Mock
	private DockerCommitDao mockDockerCommitDao;
	@Mock
	private TransactionalMessenger mockTransactionalMessenger;
	
	@InjectMocks
	private SubmissionManagerImpl submissionManager;	
	
	private Folder folder;
	private EntityBundle bundle;
	
    private FileHandle fileHandle1;
    private FileHandle fileHandle2;
	private List<String> handleIds;
	
	private static final String EVAL_ID = "12";
	private static final Long EVAL_ID_LONG = Long.parseLong("12");
	private static final String OWNER_ID = "34";
	private static final String USER_ID = "56";
	private static final String SUB_ID = "78";
	private static final String SUB2_ID = "87";
	private static final String PROJECT_ID = "syn56";
	private static final String ENTITY_ID = "90";
	private static final String ENTITY2_ID = "99";
	private static final String DOCKER_REPO_ENTITY_ID = "11";
	private static final String ETAG = "etag";	
	private static final String HANDLE_ID_2 = "handle2";
	private static final String HANDLE_ID_1 = "handle1";
	private static final String TEST_URL = "http://www.foo.com/bar";
	private static final String TEAM_ID = "999";
	private static final String CHALLENGE_END_POINT = "https://synapse.org/#ENTITY:";
	private static final String NOTIFICATION_UNSUBSCRIBE_END_POINT = "https://synapse.org/#notificationUnsubscribeEndpoint:";
	private static final String DOCKER_REPO_NAME = "docker.synapse.org/syn12345/arepo";
	private static final String DOCKER_REPO_DIGEST = "abcdef012345";
	
	private static UserInfo ownerInfo;
	private static UserInfo userInfo;
	
	private static EntityBundle createDockerEntityBundle() {
		EntityBundle bundle = new EntityBundle();
		DockerRepository entity = new DockerRepository();
		entity.setRepositoryName(DOCKER_REPO_NAME);
		bundle.setEntity(entity);
		bundle.setFileHandles(Collections.EMPTY_LIST);
		return bundle;
	}
	
	private static String createEntityBundleJSON(EntityBundle bundle) throws JSONObjectAdapterException {
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		bundle.writeToJSONObject(joa);
		return joa.toJSONString();
	}
	
    @BeforeEach
    public void setUp() throws Exception {
		// User Info
    	ownerInfo = new UserInfo(false, OWNER_ID);
    	userInfo = new UserInfo(false, USER_ID);
    	
    	// FileHandles
		List<FileHandle> handles = new ArrayList<FileHandle>();
		handleIds = new ArrayList<String>();
		
		fileHandle1 = new S3FileHandle();
		fileHandle1.setId(HANDLE_ID_1);
		handles.add(fileHandle1);
		handleIds.add(HANDLE_ID_1);
		fileHandle2 = new S3FileHandle();
		fileHandle2.setId(HANDLE_ID_2);
		handles.add(fileHandle2);
		handleIds.add(HANDLE_ID_2);
    	
    	// Objects
		eval = new Evaluation();
		eval.setName("compName");
		eval.setId(EVAL_ID);
		eval.setOwnerId(OWNER_ID);
        eval.setContentSource(PROJECT_ID);
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
        dockerBundle = createDockerEntityBundle();
    	sub.setEntityBundleJSON(createEntityBundleJSON(dockerBundle));
        
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
        subStatus.setAnnotations(createDummyAnnotations().getFirst());
        
        submissionBundle = new SubmissionBundle();
        submissionBundle.setSubmission(subWithId);
        submissionBundle.setSubmissionStatus(subStatus);
        
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
    }
	
	@Test
	public void testCRUDAsAdmin() throws Exception {
    	when(mockIdGenerator.generateNewId(IdType.EVALUATION_SUBMISSION_ID)).thenReturn(Long.parseLong(SUB_ID));
    	when(mockSubmissionDAO.get(eq(SUB_ID))).thenReturn(subWithId);
    	when(mockSubmissionDAO.create(eq(sub))).thenReturn(SUB_ID);
    	when(mockSubmissionStatusDAO.get(eq(SUB_ID))).thenReturn(subStatus);
    	when(mockNode.getNodeType()).thenReturn(EntityType.file);
    	when(mockNode.getETag()).thenReturn(ETAG);
    	when(mockNodeManager.getNode(any(UserInfo.class), eq(ENTITY_ID))).thenReturn(mockNode);
    	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.SUBMIT))).thenReturn(AuthorizationStatus.authorized());
    	when(mockEvalPermissionsManager.hasAccess(eq(ownerInfo), eq(EVAL_ID), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
    	when(mockSubmissionStatusDAO.getEvaluationIdForBatch((List<SubmissionStatus>)anyObject())).thenReturn(Long.parseLong(EVAL_ID));

    	// by default we say that individual submissions are within quota
    	// (specific tests will change this)
    	when(mockSubmissionEligibilityManager.isIndividualEligible(eq(EVAL_ID), any(UserInfo.class), any(Date.class))).
    		thenReturn(AuthorizationStatus.authorized());
    	
		assertNull(sub.getId());
		assertNotNull(subWithId.getId());
		submissionManager.createSubmission(userInfo, sub, ETAG, null, bundle);
		submissionManager.getSubmission(ownerInfo, SUB_ID);
		submissionManager.updateSubmissionStatus(ownerInfo, subStatus);
		submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
		
		// add another contributor as an admin
		SubmissionContributor submissionContributor = new SubmissionContributor();
		submissionContributor.setPrincipalId("101");
		SubmissionContributor scCreated = submissionManager.addSubmissionContributor(new UserInfo(true), SUB_ID, submissionContributor);
		assertEquals("101", scCreated.getPrincipalId());
		assertNotNull(scCreated.getCreatedOn());
		
		submissionManager.deleteSubmission(ownerInfo, SUB_ID);
		verify(mockSubmissionDAO).create(any(Submission.class));
		verify(mockSubmissionDAO).delete(eq(SUB_ID));
		verify(mockSubmissionStatusDAO).create(any(SubmissionStatus.class));
		verify(mockTransactionalMessenger).sendMessageAfterCommit(getSubmissionMessage(userInfo, SUB_ID, ChangeType.CREATE));
		verify(mockSubmissionStatusDAO, times(2)).update(any(List.class));
		verify(mockTransactionalMessenger, times(2)).sendMessageAfterCommit(getSubmissionMessage(ownerInfo, SUB_ID, ChangeType.UPDATE));
		verify(mockSubmissionFileHandleDAO).create(eq(SUB_ID), eq(fileHandle1.getId()));
		verify(mockSubmissionFileHandleDAO).create(eq(SUB_ID), eq(fileHandle2.getId()));
		verify(mockEvaluationSubmissionsDAO, times(1)).lockAndGetForEvaluation(EVAL_ID_LONG);
		verify(mockEvaluationSubmissionsDAO, times(1)).updateEtagForEvaluation(EVAL_ID_LONG, true, ChangeType.CREATE);
		verify(mockEvaluationSubmissionsDAO, times(3)).updateEtagForEvaluation(EVAL_ID_LONG, true, ChangeType.UPDATE);
	}
	
	@Test
	public void testCAsUser_NotAuthorized() throws Exception {
		assertNull(sub.getId());
		assertNotNull(subWithId.getId());
		when(mockEvalPermissionsManager.hasAccess(
				eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.SUBMIT))).thenReturn(AuthorizationStatus.accessDenied(""));
		
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			submissionManager.createSubmission(userInfo, sub, ETAG, null, bundle);		
		});
	}

	@Test
	public void testCreateSubmissionAsUser_NonNullEvaluationRoundId() throws Exception {
		sub.setEvaluationRoundId("2234");

		String message = assertThrows(IllegalArgumentException.class, () -> {
			submissionManager.createSubmission(userInfo, sub, ETAG, null, bundle);
		}).getMessage();

		assertEquals("Please do not specify any evaluationRoundId. This will be filled automatically based upon the time of creation.", message);
	}
	
	@Test
	public void testCRUDAsUser() throws NotFoundException, DatastoreException, JSONObjectAdapterException {
		when(mockIdGenerator.generateNewId(IdType.EVALUATION_SUBMISSION_ID)).thenReturn(Long.parseLong(SUB_ID));
    	when(mockSubmissionDAO.get(eq(SUB_ID))).thenReturn(subWithId);
    	when(mockSubmissionDAO.create(eq(sub))).thenReturn(SUB_ID);
    	when(mockNode.getNodeType()).thenReturn(EntityType.file);
    	when(mockNode.getETag()).thenReturn(ETAG);
    	when(mockNodeManager.getNode(any(UserInfo.class), eq(ENTITY_ID))).thenReturn(mockNode);    	    	
    	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.SUBMIT))).thenReturn(AuthorizationStatus.authorized());
       	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.READ_PRIVATE_SUBMISSION))).thenReturn(AuthorizationStatus.accessDenied(""));
       	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.UPDATE_SUBMISSION))).thenReturn(AuthorizationStatus.accessDenied(""));
       	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.DELETE_SUBMISSION))).thenReturn(AuthorizationStatus.accessDenied(""));
    	when(mockSubmissionStatusDAO.getEvaluationIdForBatch((List<SubmissionStatus>)anyObject())).thenReturn(Long.parseLong(EVAL_ID));


    	// by default we say that individual submissions are within quota
    	// (specific tests will change this)
    	when(mockSubmissionEligibilityManager.isIndividualEligible(eq(EVAL_ID), any(UserInfo.class), any(Date.class))).
    		thenReturn(AuthorizationStatus.authorized());
    	
		assertNull(sub.getId());
		assertNotNull(subWithId.getId());
		submissionManager.createSubmission(userInfo, sub, ETAG, null, bundle);

		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			submissionManager.getSubmission(userInfo, SUB_ID);
		});
		
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			submissionManager.updateSubmissionStatus(userInfo, subStatus);
		});	

		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			submissionManager.updateSubmissionStatusBatch(userInfo, EVAL_ID, batch);
		});
		
		// add another contributor
		SubmissionContributor submissionContributor = new SubmissionContributor();
		submissionContributor.setPrincipalId("101");

		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			submissionManager.addSubmissionContributor(userInfo, SUB_ID, submissionContributor);
		});
		
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			submissionManager.deleteSubmission(userInfo, SUB_ID);
		});
		
		verify(mockSubmissionDAO).create(any(Submission.class));
		verify(mockSubmissionDAO, never()).delete(eq(SUB_ID));
		verify(mockSubmissionStatusDAO).create(any(SubmissionStatus.class));
		verify(mockTransactionalMessenger).sendMessageAfterCommit(getSubmissionMessage(userInfo, SUB_ID, ChangeType.CREATE));
		verify(mockSubmissionStatusDAO, never()).update(any(List.class));
	}
	
	@Test
	public void testInsertUserAsContributor_NullContributorList() throws Exception {
		when(mockIdGenerator.generateNewId(IdType.EVALUATION_SUBMISSION_ID)).thenReturn(Long.parseLong(SUB_ID));
    	when(mockSubmissionDAO.get(eq(SUB_ID))).thenReturn(subWithId);
    	when(mockSubmissionDAO.create(eq(sub))).thenReturn(SUB_ID);
    	when(mockNode.getNodeType()).thenReturn(EntityType.file);
    	when(mockNode.getETag()).thenReturn(ETAG);
    	when(mockNodeManager.getNode(any(UserInfo.class), eq(ENTITY_ID))).thenReturn(mockNode);    	
    	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.SUBMIT))).thenReturn(AuthorizationStatus.authorized());
    	// by default we say that individual submissions are within quota
    	// (specific tests will change this)
    	when(mockSubmissionEligibilityManager.isIndividualEligible(eq(EVAL_ID), any(UserInfo.class), any(Date.class))).
    		thenReturn(AuthorizationStatus.authorized());
    	
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
		when(mockIdGenerator.generateNewId(IdType.EVALUATION_SUBMISSION_ID)).thenReturn(Long.parseLong(SUB_ID));
    	when(mockSubmissionDAO.get(eq(SUB_ID))).thenReturn(subWithId);
    	when(mockSubmissionDAO.create(eq(sub))).thenReturn(SUB_ID);
    	when(mockNode.getNodeType()).thenReturn(EntityType.file);
    	when(mockNode.getETag()).thenReturn(ETAG);
    	when(mockNodeManager.getNode(any(UserInfo.class), eq(ENTITY_ID))).thenReturn(mockNode);
    	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.SUBMIT))).thenReturn(AuthorizationStatus.authorized());

    	// by default we say that individual submissions are within quota
    	// (specific tests will change this)
    	when(mockSubmissionEligibilityManager.isIndividualEligible(eq(EVAL_ID), any(UserInfo.class), any(Date.class))).
    		thenReturn(AuthorizationStatus.authorized());
    	
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
		when(mockIdGenerator.generateNewId(IdType.EVALUATION_SUBMISSION_ID)).thenReturn(Long.parseLong(SUB_ID));
    	when(mockSubmissionDAO.get(eq(SUB_ID))).thenReturn(subWithId);
    	when(mockSubmissionDAO.create(eq(sub))).thenReturn(SUB_ID);
    	when(mockNode.getNodeType()).thenReturn(EntityType.file);
    	when(mockNode.getETag()).thenReturn(ETAG);
    	when(mockNodeManager.getNode(any(UserInfo.class), eq(ENTITY_ID))).thenReturn(mockNode);    	
    	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.SUBMIT))).thenReturn(AuthorizationStatus.authorized());
    	
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
				thenReturn(AuthorizationStatus.authorized());
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
	
	@Test
	public void testContributorListButNoTeamId() throws Exception {
    	when(mockNode.getNodeType()).thenReturn(EntityType.file);
    	when(mockNode.getETag()).thenReturn(ETAG);
    	when(mockNodeManager.getNode(any(UserInfo.class), eq(ENTITY_ID))).thenReturn(mockNode);    	
    	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.SUBMIT))).thenReturn(AuthorizationStatus.authorized());
    	
		assertNull(sub.getContributors());
		Set<SubmissionContributor> contributors = new HashSet<SubmissionContributor>();
		SubmissionContributor sc = new SubmissionContributor();
		sc.setPrincipalId(OWNER_ID);
		contributors.add(sc);
		sub.setContributors(contributors);
		Assertions.assertThrows(InvalidModelException.class, ()-> {
			submissionManager.createSubmission(userInfo, sub, ETAG, null, bundle);
		});
	}
	
	@Test
	public void testIndividualSubmissionWithHash() throws Exception {
    	when(mockNode.getNodeType()).thenReturn(EntityType.file);
    	when(mockNode.getETag()).thenReturn(ETAG);
    	when(mockNodeManager.getNode(any(UserInfo.class), eq(ENTITY_ID))).thenReturn(mockNode);
    	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.SUBMIT))).thenReturn(AuthorizationStatus.authorized());
    	
		assertNull(sub.getContributors());
		Assertions.assertThrows(InvalidModelException.class, ()-> {
			submissionManager.createSubmission(userInfo, sub, ETAG, "123456", bundle);
		});
	}
	
	@Test
	public void testUnauthorizedGet() throws NotFoundException, DatastoreException, JSONObjectAdapterException {
    	when(mockSubmissionDAO.get(eq(SUB2_ID))).thenReturn(sub2WithId);
       	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.READ_PRIVATE_SUBMISSION))).thenReturn(AuthorizationStatus.accessDenied(""));
    	
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			submissionManager.getSubmission(userInfo, SUB2_ID);
		});
	}
	
	@Test
	public void testUnauthorizedEntity() throws NotFoundException, DatastoreException, JSONObjectAdapterException {
    	when(mockNodeManager.getNode(eq(userInfo), eq(ENTITY2_ID))).thenThrow(new UnauthorizedException());
    	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.SUBMIT))).thenReturn(AuthorizationStatus.authorized());
       
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			// user should not have access to sub2
			submissionManager.createSubmission(userInfo, sub2, ETAG, null, bundle);		
		});
	}
	
	@Test
	public void testCreateWithErroneousDockerDigest() throws Exception{
		when(mockIdGenerator.generateNewId(IdType.EVALUATION_SUBMISSION_ID)).thenReturn(Long.parseLong(SUB_ID));
    	when(mockSubmissionDAO.get(eq(SUB_ID))).thenReturn(subWithId);
    	when(mockSubmissionDAO.create(eq(sub))).thenReturn(SUB_ID);
    	when(mockNode.getNodeType()).thenReturn(EntityType.file);
    	when(mockNode.getETag()).thenReturn(ETAG);
    	when(mockNodeManager.getNode(any(UserInfo.class), eq(ENTITY_ID))).thenReturn(mockNode);    	
    	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.SUBMIT))).thenReturn(AuthorizationStatus.authorized());
    	when(mockEvalPermissionsManager.hasAccess(eq(ownerInfo), eq(EVAL_ID), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
    	when(mockSubmissionEligibilityManager.isIndividualEligible(eq(EVAL_ID), any(UserInfo.class), any(Date.class))).
    		thenReturn(AuthorizationStatus.authorized());
    	
		sub.setDockerDigest("this is some digest");
		submissionManager.createSubmission(userInfo, sub, ETAG, null, bundle);
		Submission retrieved = submissionManager.getSubmission(ownerInfo, SUB_ID);
		assertNull(retrieved.getDockerDigest());
	}
	
	@Test
	public void testCreateDockerRepoSubmission() throws Exception {
		when(mockIdGenerator.generateNewId(IdType.EVALUATION_SUBMISSION_ID)).thenReturn(Long.parseLong(SUB_ID));
    	when(mockSubmissionDAO.get(eq(SUB_ID))).thenReturn(subWithId);
    	when(mockSubmissionDAO.create(eq(sub))).thenReturn(SUB_ID);
    	when(mockDockerRepoNode.getNodeType()).thenReturn(EntityType.dockerrepo);
    	when(mockDockerRepoNode.getETag()).thenReturn(ETAG);
    	when(mockNodeManager.getNode(any(UserInfo.class), eq(DOCKER_REPO_ENTITY_ID))).thenReturn(mockDockerRepoNode);
    	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.SUBMIT))).thenReturn(AuthorizationStatus.authorized());    	when(mockSubmissionEligibilityManager.isIndividualEligible(eq(EVAL_ID), any(UserInfo.class), any(Date.class))).
    		thenReturn(AuthorizationStatus.authorized());
    	
    	DockerCommit commit = new DockerCommit();
    	commit.setTag("foo");
    	commit.setDigest(DOCKER_REPO_DIGEST);
    	when(mockDockerCommitDao.
    			listCommitsByOwnerAndDigest(DOCKER_REPO_ENTITY_ID, DOCKER_REPO_DIGEST)).
    			thenReturn(Collections.singletonList(commit));
    	
		sub.setEntityId(DOCKER_REPO_ENTITY_ID);
		sub.setDockerDigest(DOCKER_REPO_DIGEST);
		submissionManager.createSubmission(userInfo, sub, ETAG, null, dockerBundle);		
		verify(mockDockerCommitDao).listCommitsByOwnerAndDigest(DOCKER_REPO_ENTITY_ID, DOCKER_REPO_DIGEST);
	}
	
	@Test
	public void testCreateDockerRepoSubmissionNoDigest() throws Exception {
    	when(mockDockerRepoNode.getNodeType()).thenReturn(EntityType.dockerrepo);
    	when(mockDockerRepoNode.getETag()).thenReturn(ETAG);
    	when(mockNodeManager.getNode(any(UserInfo.class), eq(DOCKER_REPO_ENTITY_ID))).thenReturn(mockDockerRepoNode);    	
    	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.SUBMIT))).thenReturn(AuthorizationStatus.authorized());
    	
		sub.setEntityId(DOCKER_REPO_ENTITY_ID);
		sub.setDockerDigest(null);
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			submissionManager.createSubmission(userInfo, sub, ETAG, null, dockerBundle);		
		});
	}
	
	@Test
	public void testCreateDockerRepoSubmissionInvalidDigest() throws Exception {
		when(mockDockerRepoNode.getNodeType()).thenReturn(EntityType.dockerrepo);
    	when(mockDockerRepoNode.getETag()).thenReturn(ETAG);
    	when(mockNodeManager.getNode(any(UserInfo.class), eq(DOCKER_REPO_ENTITY_ID))).thenReturn(mockDockerRepoNode);    	
    	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.SUBMIT))).thenReturn(AuthorizationStatus.authorized());
    	
		sub.setEntityId(DOCKER_REPO_ENTITY_ID);
		sub.setDockerDigest("not a valid digest");
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			submissionManager.createSubmission(userInfo, sub, ETAG, null, dockerBundle);
		});
	}
	
	@Test
	public void testInvalidScore() throws Exception {
    	when(mockSubmissionDAO.get(eq(SUB_ID))).thenReturn(subWithId);
    	when(mockEvalPermissionsManager.hasAccess(eq(ownerInfo), eq(EVAL_ID), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
		
    	subStatus.setScore(1.1);
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			submissionManager.updateSubmissionStatus(ownerInfo, subStatus);
		});
	}
		
	@Test
	public void testInvalidEtag() throws Exception {
    	when(mockNode.getETag()).thenReturn(ETAG);
    	when(mockNodeManager.getNode(any(UserInfo.class), eq(ENTITY_ID))).thenReturn(mockNode);    	
    	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.SUBMIT))).thenReturn(AuthorizationStatus.authorized());
       	
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			submissionManager.createSubmission(userInfo, sub, ETAG + "modified", null, bundle);
		});
	}
	
	@Test
	public void testGetAllSubmissions() throws DatastoreException, UnauthorizedException, NotFoundException {
    	when(mockEvalPermissionsManager.hasAccess(eq(ownerInfo), eq(EVAL_ID), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
    	
		SubmissionStatusEnum statusEnum = SubmissionStatusEnum.SCORED;
		submissionManager.getAllSubmissions(ownerInfo, EVAL_ID, null, 10, 0);
		submissionManager.getAllSubmissions(ownerInfo, EVAL_ID, statusEnum, 10, 0);
		verify(mockSubmissionDAO).getAllByEvaluation(eq(EVAL_ID), anyLong(), anyLong());
		verify(mockSubmissionDAO).getAllByEvaluationAndStatus(eq(EVAL_ID), eq(statusEnum), eq(10L), eq(0L));
	}
	
	@Test
	public void testGetAllSubmissionsUnauthorized() throws DatastoreException, UnauthorizedException, NotFoundException {
    	when(mockEvalPermissionsManager.hasAccess(eq(ownerInfo), eq(USER_ID), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.accessDenied(""));
    	Assertions.assertThrows(UnauthorizedException.class, ()-> {
    		submissionManager.getAllSubmissions(ownerInfo, USER_ID, null, 10, 0);
    	});
	}
	
	@Test
	public void testGetAllSubmissionBundles() throws Exception {
		when(mockEvalPermissionsManager.hasAccess(eq(ownerInfo), eq(EVAL_ID), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
    			
		SubmissionStatusEnum statusEnum = SubmissionStatusEnum.SCORED;
		when(mockSubmissionDAO.getAllBundlesByEvaluationAndStatus(EVAL_ID, statusEnum, 10L, 0L)).
			thenReturn(Collections.singletonList(submissionBundle));
		
		List<SubmissionBundle> queryResults = 
				submissionManager.getAllSubmissionBundles(ownerInfo, EVAL_ID, statusEnum, 10L, 0L);
		assertEquals(1L, queryResults.size());
		SubmissionBundle expected = new SubmissionBundle();
		expected.setSubmission(subWithId);
		expected.setSubmissionStatus(subStatus);
		assertEquals(Collections.singletonList(expected), queryResults);
		verify(mockSubmissionDAO).getAllBundlesByEvaluationAndStatus(eq(EVAL_ID), eq(statusEnum), eq(10L), eq(0L));
	}
	
	@Test
	public void testGetMyOwnSubmissionBundles() throws Exception {
		when(mockEvalPermissionsManager.hasAccess(eq(ownerInfo), eq(EVAL_ID), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
    	
		when(mockSubmissionDAO.getAllBundlesByEvaluationAndUser(EVAL_ID, ownerInfo.getId().toString(), 10L, 0L)).
			thenReturn(Collections.singletonList(submissionBundle));
		List<SubmissionBundle> queryResults = 
				submissionManager.getMyOwnSubmissionBundlesByEvaluation(ownerInfo, EVAL_ID, 10L, 0L);
		SubmissionBundle expected = new SubmissionBundle();
		expected.setSubmission(subWithId);
		expected.setSubmissionStatus(subStatus);
		assertEquals(Collections.singletonList(expected), queryResults);
		verify(mockSubmissionDAO).getAllBundlesByEvaluationAndUser(EVAL_ID, ownerInfo.getId().toString(), 10L, 0L);
	}
	
	@Test
	public void testGetMyOwnSubmissions() throws Exception {
		submissionManager.getMyOwnSubmissionsByEvaluation(ownerInfo, EVAL_ID, 10, 0);
		verify(mockSubmissionDAO).getAllByEvaluationAndUser(EVAL_ID, ownerInfo.getId().toString(), 10, 0);
	}
	
	@Test
	public void testGetSubmissionCount() throws DatastoreException, NotFoundException {
		when(mockEvalPermissionsManager.hasAccess(eq(ownerInfo), eq(EVAL_ID), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
    	
		submissionManager.getSubmissionCount(ownerInfo, EVAL_ID);
		verify(mockSubmissionDAO).getCountByEvaluation(eq(EVAL_ID));
	}
	
	@Test
	public void testGetRedirectURLForFileHandle()
			throws DatastoreException, NotFoundException {
		when(mockSubmissionDAO.get(eq(SUB_ID))).thenReturn(subWithId);
    	when(mockSubmissionFileHandleDAO.getAllBySubmission(eq(SUB_ID))).thenReturn(handleIds);
    	when(mockEvalPermissionsManager.hasAccess(eq(ownerInfo), eq(EVAL_ID), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
    	
		
		FileHandleUrlRequest urlRequest = new FileHandleUrlRequest(ownerInfo, HANDLE_ID_1)
				.withAssociation(FileHandleAssociateType.SubmissionAttachment, SUB_ID);
		
		when(mockFileHandleManager.getRedirectURLForFileHandle(eq(urlRequest))).thenReturn(TEST_URL);
		
		String url = submissionManager.getRedirectURLForFileHandle(ownerInfo, SUB_ID, HANDLE_ID_1);
		
		verify(mockSubmissionFileHandleDAO).getAllBySubmission(eq(SUB_ID));
		verify(mockFileHandleManager).getRedirectURLForFileHandle(eq(urlRequest));

		assertEquals(TEST_URL, url);
	}
	
	@Test
	public void testGetRedirectURLForFileHandleUnauthorized()
			throws DatastoreException, NotFoundException {
    	when(mockSubmissionDAO.get(eq(SUB_ID))).thenReturn(subWithId);
    	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.READ_PRIVATE_SUBMISSION))).thenReturn(AuthorizationStatus.accessDenied(""));
       	
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			submissionManager.getRedirectURLForFileHandle(userInfo, SUB_ID, HANDLE_ID_1);
		});
	}
	
	@Test
	public void testGetRedirectURLForFileHandleNotFound() 
			throws DatastoreException, NotFoundException {
    	when(mockSubmissionDAO.get(eq(SUB2_ID))).thenReturn(sub2WithId);
    	when(mockEvalPermissionsManager.hasAccess(eq(ownerInfo), eq(EVAL_ID), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
    	when(mockSubmissionFileHandleDAO.getAllBySubmission(any())).thenReturn(Collections.emptyList());
    	   	
		Assertions.assertThrows(NotFoundException.class, ()-> {
			// HANDLE_ID_1 is not contained in this Submission
			submissionManager.getRedirectURLForFileHandle(ownerInfo, SUB2_ID, HANDLE_ID_1);
		});
	}
	
	@Test
	public void testGetSubmissionStatus() throws DatastoreException, NotFoundException {
		when(mockEvalPermissionsManager.hasAccess(eq(ownerInfo), eq(EVAL_ID), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
    	when(mockSubmissionDAO.getBundle(any(), anyBoolean())).thenReturn(submissionBundle);
		
		SubmissionStatus status = submissionManager.getSubmissionStatus(ownerInfo, SUB_ID);
		
		verify(mockSubmissionDAO).getBundle(SUB_ID, false);
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
	
	@Test
	public void testGetSubmissionStatusNoREADAccess() throws Exception {
		when(mockSubmissionDAO.getBundle(any(), anyBoolean())).thenReturn(submissionBundle);
    	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.READ))).thenReturn(AuthorizationStatus.accessDenied(""));
    	Assertions.assertThrows(UnauthorizedException.class, ()-> {
    		submissionManager.getSubmissionStatus(userInfo, SUB_ID);
    	});
	}
	
	@Test
	public void testGetALLSubmissionStatus() throws Exception {
       	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.READ))).thenReturn(AuthorizationStatus.authorized());
       	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.READ_PRIVATE_SUBMISSION))).thenReturn(AuthorizationStatus.accessDenied(""));
		when(mockSubmissionDAO.getAllBundlesByEvaluation(EVAL_ID, 100L, 0L)).thenReturn(Collections.singletonList(submissionBundle));
		List<SubmissionStatus> actual = submissionManager.getAllSubmissionStatuses(userInfo, EVAL_ID, null, 100L, 0L);
		verify(mockSubmissionDAO).getAllBundlesByEvaluation(EVAL_ID, 100L, 0L);
		List<SubmissionStatus> expected = Collections.singletonList(subStatus);
		assertEquals(expected, actual);
	}
	
	@Test
	public void testGetALLSubmissionStatusNoREADAccess() throws Exception {
    	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.READ))).thenReturn(AuthorizationStatus.accessDenied(""));
    	Assertions.assertThrows(UnauthorizedException.class, ()-> {
    		submissionManager.getAllSubmissionStatuses(userInfo, EVAL_ID, null, 100L, 0L);
    	});
	}
	
	@Test
	public void testGetSubmissionStatusNoPrivate() throws DatastoreException, NotFoundException {
    	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.READ))).thenReturn(AuthorizationStatus.authorized());
       	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.READ_PRIVATE_SUBMISSION))).thenReturn(AuthorizationStatus.accessDenied(""));
       	when(mockSubmissionDAO.getBundle(any(), anyBoolean())).thenReturn(submissionBundle);
    	
		SubmissionStatus status = submissionManager.getSubmissionStatus(userInfo, SUB_ID);

		verify(mockSubmissionDAO).getBundle(SUB_ID, false);
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
		when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.READ))).thenReturn(AuthorizationStatus.authorized());
       	when(mockEvalPermissionsManager.hasAccess(eq(userInfo), eq(EVAL_ID), eq(ACCESS_TYPE.READ_PRIVATE_SUBMISSION))).thenReturn(AuthorizationStatus.accessDenied(""));
       	when(mockSubmissionDAO.getBundle(any(), anyBoolean())).thenReturn(submissionBundle);
       	
 		Annotations annots = subStatus.getAnnotations();
		annots.setStringAnnos(null); // this is the case in PLFM-2586
		annots.setLongAnnos(null);
		annots.setDoubleAnnos(null);
		submissionManager.getSubmissionStatus(userInfo, SUB_ID);
		verify(mockSubmissionDAO).getBundle(SUB_ID, false);
	}
	
	private static Pair<Annotations, org.sagebionetworks.repo.model.annotation.v2.Annotations> createDummyAnnotations() {		
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
		
		org.sagebionetworks.repo.model.annotation.Annotations annos = new org.sagebionetworks.repo.model.annotation.Annotations();
		annos.setStringAnnos(stringAnnos);
		annos.setLongAnnos(longAnnos);
		annos.setDoubleAnnos(doubleAnnos);
		
		org.sagebionetworks.repo.model.annotation.v2.Annotations expected = AnnotationsV2Utils.emptyAnnotations();
		
		AnnotationsV2TestUtils.putAnnotations(expected, "sa", "foo", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(expected, "la", "42", AnnotationsValueType.LONG);
		AnnotationsV2TestUtils.putAnnotations(expected, "doa", "3.14", AnnotationsValueType.DOUBLE);
		
		return Pair.create(annos, expected);
	}

	@Test
	public void testBadBatch() throws Exception {
		when(mockEvalPermissionsManager.hasAccess(eq(ownerInfo), eq(EVAL_ID), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
    	when(mockSubmissionStatusDAO.getEvaluationIdForBatch((List<SubmissionStatus>)anyObject())).thenReturn(Long.parseLong(EVAL_ID));

		// baseline:  all is good
		submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
		
		batch.setIsFirstBatch(null);
		
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
		});

		batch.setIsFirstBatch(true);
		submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
		
		batch.setIsLastBatch(null);
		
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
		});

		batch.setIsLastBatch(true);
		submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
		
		List<SubmissionStatus> statuses = new ArrayList<SubmissionStatus>(batch.getStatuses());
		batch.getStatuses().clear();
		
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
		});

		batch.setStatuses(null);
		
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
		});

		List<SubmissionStatus> tooLong = new ArrayList<SubmissionStatus>();
		for (int i=0; i<501; i++) tooLong.add(new SubmissionStatus());
		batch.setStatuses(tooLong);

		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
		});
		
		List<SubmissionStatus> hasNull = new ArrayList<SubmissionStatus>();
		hasNull.add(null);
		batch.setStatuses(hasNull);
		
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
		});
		
		// back to base-line
		batch.setStatuses(statuses);
		submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
		
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID+"xxx", batch);
		});
	}
	
	@Test
	public void testConflictingBatchUpdate() throws Exception {
		when(mockEvalPermissionsManager.hasAccess(eq(ownerInfo), eq(EVAL_ID), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
    	when(mockSubmissionStatusDAO.getEvaluationIdForBatch((List<SubmissionStatus>)anyObject())).thenReturn(Long.parseLong(EVAL_ID));

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
		
		Assertions.assertThrows(ConflictingUpdateException.class, ()-> {
			submissionManager.updateSubmissionStatusBatch(ownerInfo, EVAL_ID, batch);
		});
	}
	
	@Test
	public void testBatchResponseToken() throws Exception {
		when(mockEvalPermissionsManager.hasAccess(eq(ownerInfo), eq(EVAL_ID), any(ACCESS_TYPE.class))).thenReturn(AuthorizationStatus.authorized());
    	when(mockSubmissionStatusDAO.getEvaluationIdForBatch((List<SubmissionStatus>)anyObject())).thenReturn(Long.parseLong(EVAL_ID));

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
	
	@Test
	public void testCreateSubmissionNotification() throws Exception {
		assertNull(sub.getContributors());
		Set<SubmissionContributor> contributors = new HashSet<SubmissionContributor>();
		SubmissionContributor sc = new SubmissionContributor();
		sc.setPrincipalId(USER_ID);
		contributors.add(sc);
		SubmissionContributor sc2 = new SubmissionContributor();
		sc2.setPrincipalId("99");
		contributors.add(sc2);
		sub.setContributors(contributors);
		sub.setTeamId(TEAM_ID);
		int submissionEligibilityHash = 1234;
		
		Team team = new Team();
		team.setName("test team");
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);
		
		Evaluation evaluation = new Evaluation();
		evaluation.setContentSource("syn101");
		String evalQueueName = "express queue";
		evaluation.setName(evalQueueName);
		when(mockEvaluationDAO.get(sub.getEvaluationId())).thenReturn(evaluation);
		
		UserProfile up = new UserProfile();
		up.setUserName("auser");
		when(mockUserProfileManager.getUserProfile(userInfo.getId().toString())).thenReturn(up);

		List<MessageToUserAndBody> result = 
				submissionManager.createSubmissionNotifications(
						userInfo, sub, ""+submissionEligibilityHash,
						CHALLENGE_END_POINT,
						NOTIFICATION_UNSUBSCRIBE_END_POINT);
		assertEquals(1, result.size());
		assertEquals("Team Challenge Submission", result.get(0).getMetadata().getSubject());
		assertEquals("test team <testteam@synapse.org>", result.get(0).getMetadata().getTo());
		assertEquals(Collections.singleton("99"), result.get(0).getMetadata().getRecipients());
		String body = result.get(0).getBody();
		String userId = USER_ID;
		String displayName = "auser";
		String challengeName = "syn101";
		String challengeWebLink = CHALLENGE_END_POINT + "syn101";
		String teamId = TEAM_ID;
		String teamName = "test team";
		String expected = "<html style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;font-size: 10px;-webkit-tap-highlight-color: rgba(0, 0, 0, 0);\">\r\n" + 
				"  <body style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;font-family: &quot;Helvetica Neue&quot;, Helvetica, Arial, sans-serif;font-size: 14px;line-height: 1.42857143;color: #333333;background-color: #ffffff;\">\r\n" + 
				"    <div style=\"margin: 10px;-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;\">\r\n" + 
				"      <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;margin-bottom: 20px;font-size: 16px;font-weight: 300;line-height: 1.4;\">Hello,</p>\r\n" + 
				"      <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\">\r\n" + 
				"        <strong style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;font-weight: bold;\"><a href=\"https://www.synapse.org/#!Profile:" + userId + "\">" + displayName + "</a></strong>\r\n" + 
				"        has created a submission to\r\n" + 
				"        <strong style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;font-weight: bold;\">" + challengeName + "</strong>\r\n" +
				"        (" + evalQueueName + ")\r\n" +
				"        on behalf of\r\n" + 
				"        <strong style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;font-weight: bold;\"><a href=\"https://www.synapse.org/#!Team:" + teamId + "\">" + teamName + "</a></strong>.\r\n" + 
				"      </p>\r\n" + 
				"      <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\">For further information please <a href=\"" + challengeWebLink + "\" style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;background-color: transparent;color: #337ab7;text-decoration: none;\">visit the challenge page</a>.</p>\r\n" + 
				"      <br style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;\">\r\n" + 
				"      <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\">\r\n" + 
				"        Sincerely,\r\n" + 
				"      </p>\r\n" + 
				"      <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\">\r\n" + 
				"        <img src=\"https://s3.amazonaws.com/static.synapse.org/images/SynapseLogo2.png\" style=\"display: inline;width: 40px;height: 40px;-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;border: 0;vertical-align: middle;\"> Synapse Administration\r\n" + 
				"      </p>\r\n" + 
				"    </div>\r\n" + 
				"  </body>\r\n" + 
				"</html>\r\n";
		assertEquals(expected, body);
	}
	
	@Test
	public void testCreateSubmissionNotification_Individual() throws Exception {
		// check that when it's not a team submission no notification is created
		sub.setTeamId(null);
		List<MessageToUserAndBody> result = 
				submissionManager.createSubmissionNotifications(userInfo, sub, null,
						CHALLENGE_END_POINT,
						NOTIFICATION_UNSUBSCRIBE_END_POINT);
		assertTrue(result.isEmpty());
		
	}
	
	@Test
	public void testBuildSubmissionNotificationBody() {
		String to = "Test team";
		String messageContent = "Content";
		String notificationUnsubscribeEndpoint = "someUnsubscribeEndpoint";
		String recipient1 = "recipient1";
		String recipient2 = "recipient2";
		
		Set<String> recipients = ImmutableSet.of(recipient1, recipient2);
		
		// Call under test
		List<MessageToUserAndBody> result = submissionManager.buildSubmissionNotificationMessages(to, recipients, messageContent, notificationUnsubscribeEndpoint);
		
		assertEquals(2, result.size());

		Set<String> resultRecipients = new HashSet<>();
		
		resultRecipients.addAll(result.get(0).getMetadata().getRecipients());
		resultRecipients.addAll(result.get(1).getMetadata().getRecipients());
		
		assertEquals(recipients, resultRecipients);
	}

	@Test
	public void testProcessCancelRequestWithInvalidUserInfo() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			submissionManager.processUserCancelRequest(null, subWithId.getId());
		});
	}

	@Test
	public void testProcessCancelRequestWithNullId() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			submissionManager.processUserCancelRequest(userInfo, null);
		});
	}

	@Test
	public void testProcessCancelRequestWithUnauthorizedUser() {
		when(mockSubmissionDAO.getCreatedBy(subWithId.getId())).thenReturn(USER_ID);
		UserInfo unauthorizedUser = new UserInfo(false);
		unauthorizedUser.setId(Long.parseLong(USER_ID+1));
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			submissionManager.processUserCancelRequest(unauthorizedUser, subWithId.getId());
		});
	}

	@Test
	public void testProcessCancelRequestWithAuthorizedUserNonCancellable() {
    	when(mockSubmissionStatusDAO.get(eq(SUB_ID))).thenReturn(subStatus);
		when(mockSubmissionDAO.getCreatedBy(subWithId.getId())).thenReturn(USER_ID);
		when(mockSubmissionDAO.getCreatedBy(subWithId.getId())).thenReturn(USER_ID);
		UserInfo authorizedUser = new UserInfo(false);
		authorizedUser.setId(Long.parseLong(USER_ID));
		
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			submissionManager.processUserCancelRequest(authorizedUser, subWithId.getId());
		});
	}

	@Test
	public void testProcessCancelRequestWithAuthorizedUserCancellable() {
		when(mockSubmissionDAO.get(eq(SUB_ID))).thenReturn(subWithId);
    	when(mockSubmissionStatusDAO.get(eq(SUB_ID))).thenReturn(subStatus);
		when(mockSubmissionDAO.getCreatedBy(subWithId.getId())).thenReturn(USER_ID);
		subStatus.setCanCancel(true);
		UserInfo authorizedUser = new UserInfo(false);
		authorizedUser.setId(Long.parseLong(USER_ID));
		
		submissionManager.processUserCancelRequest(authorizedUser, subWithId.getId());
		
		verify(mockSubmissionDAO).getCreatedBy(subWithId.getId());
		verify(mockSubmissionStatusDAO).get(subWithId.getId());
		subStatus.setCancelRequested(true);
		verify(mockSubmissionStatusDAO).update(Arrays.asList(subStatus));
		verify(mockSubmissionDAO).get(subWithId.getId());
		verify(mockEvaluationSubmissionsDAO).updateEtagForEvaluation(EVAL_ID_LONG, true, ChangeType.UPDATE);
	}
	
	
	// tests of limit and offset checks
	
	@Test
	public void testGetAllSubmissionsLimitLow() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			submissionManager.getAllSubmissions(ownerInfo, null, SubmissionStatusEnum.OPEN, -1, 0);
		});
	}

	@Test
	public void testGetAllSubmissionsLimitHigh() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			submissionManager.getAllSubmissions(ownerInfo, null, SubmissionStatusEnum.OPEN, 101, 0);
		});
	}

	@Test
	public void testGetAllSubmissionsOffsetNeg() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			submissionManager.getAllSubmissions(ownerInfo, null, SubmissionStatusEnum.OPEN, 100, -1);
		});
	}

	@Test
	public void testGetAllSubmissionBundlesLimitLow() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			submissionManager.getAllSubmissionBundles(ownerInfo, null, SubmissionStatusEnum.OPEN, -1, 0);
		});
	}

	@Test
	public void testGetAllSubmissionBundlesLimitHigh() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			submissionManager.getAllSubmissionBundles(ownerInfo, null, SubmissionStatusEnum.OPEN, 101, 0);
		});
	}

	@Test
	public void testGetAllSubmissionBundlesOffsetNeg() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			submissionManager.getAllSubmissionBundles(ownerInfo, null, SubmissionStatusEnum.OPEN, 100, -1);
		});
	}

	@Test
	public void testGetAllSubmissionStatusesLimitLow() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			submissionManager.getAllSubmissionStatuses(ownerInfo, null, SubmissionStatusEnum.OPEN, -1, 0);
		});
	}

	@Test
	public void testGetAllSubmissionStatusesLimitHigh() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			submissionManager.getAllSubmissionStatuses(ownerInfo, null, SubmissionStatusEnum.OPEN, 101, 0);
		});
	}

	@Test
	public void testGetAllSubmissionStatusesOffsetNeg() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			submissionManager.getAllSubmissionStatuses(ownerInfo, null, SubmissionStatusEnum.OPEN, 100, -1);
		});
	}

	@Test
	public void testgetMyOwnSubmissionsByEvaluationLimitLow() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			submissionManager.getMyOwnSubmissionsByEvaluation(ownerInfo, null, -1, 0);
		});
	}

	@Test
	public void testgetMyOwnSubmissionsByEvaluationLimitHigh() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			submissionManager.getMyOwnSubmissionsByEvaluation(ownerInfo, null, 101, 0);
		});
	}

	@Test
	public void testgetMyOwnSubmissionsByEvaluationOffsetNeg() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			submissionManager.getMyOwnSubmissionsByEvaluation(ownerInfo, null, 100, -1);
		});
	}
	
	@Test
	public void testgetMyOwnSubmissionBundlesByEvaluationLimitLow() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			submissionManager.getMyOwnSubmissionBundlesByEvaluation(ownerInfo, null, -1, 0);
		});
	}

	@Test
	public void testgetMyOwnSubmissionBundlesByEvaluationLimitHigh() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			submissionManager.getMyOwnSubmissionBundlesByEvaluation(ownerInfo, null, 101, 0);
		});
	}

	@Test
	public void testgetMyOwnSubmissionBundlesByEvaluationOffsetNeg() {
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			submissionManager.getMyOwnSubmissionBundlesByEvaluation(ownerInfo, null, 100, -1);
		});
	}

	@Test
	public void testValidateContentWithEmptyAnnotationsV2() {
		
		Pair<Annotations, org.sagebionetworks.repo.model.annotation.v2.Annotations> annos = createDummyAnnotations();
		
		subStatus.setAnnotations(annos.getFirst());
		
		// Call under test
		SubmissionManagerImpl.validateContent(subStatus, EVAL_ID);
		
		// Verifies that the submission annotations were not set
		assertNull(subStatus.getSubmissionAnnotations());
		
	}

	@Test
	public void testValidateContentWithoutAnnotationV1AndWithAnnotationsV2() {
		
		Pair<Annotations, org.sagebionetworks.repo.model.annotation.v2.Annotations> annos = createDummyAnnotations();
		
		subStatus.setAnnotations(null);
		subStatus.setSubmissionAnnotations(annos.getSecond());
		
		// Call under test
		SubmissionManagerImpl.validateContent(subStatus, EVAL_ID);
		
		assertNull(subStatus.getAnnotations());
		assertEquals(annos.getSecond(), subStatus.getSubmissionAnnotations());
		
	}
	
	@Test
	public void testValidateContentWithAnnotationV1AndWithAnnotationsV2() {
		
		Pair<Annotations, org.sagebionetworks.repo.model.annotation.v2.Annotations> annos = createDummyAnnotations();
		
		org.sagebionetworks.repo.model.annotation.v2.Annotations annotationsV2 = annos.getSecond();
		
		AnnotationsV2TestUtils.putAnnotations(annotationsV2, "additional", "addionalValue", AnnotationsValueType.STRING);
		
		subStatus.setAnnotations(annos.getFirst());
		subStatus.setSubmissionAnnotations(annotationsV2);
		
		// Call under test
		SubmissionManagerImpl.validateContent(subStatus, EVAL_ID);
		
		assertEquals(annos.getFirst(), subStatus.getAnnotations());
		assertEquals(annotationsV2, subStatus.getSubmissionAnnotations());
		
	}
	
	@Test
	public void testValidateContentWithMalformedAnnotationsV2() {
		
		Pair<Annotations, org.sagebionetworks.repo.model.annotation.v2.Annotations> annos = createDummyAnnotations();
		
		org.sagebionetworks.repo.model.annotation.v2.Annotations annotationsV2 = annos.getSecond();
		
		AnnotationsV2TestUtils.putAnnotations(annotationsV2, "additional", "wrongValue", AnnotationsValueType.LONG);
		
		subStatus.setSubmissionAnnotations(annotationsV2);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, ()->{
			// Call under test
			SubmissionManagerImpl.validateContent(subStatus, EVAL_ID);
		}).getMessage();
		
		assertEquals("Value associated with key=additional is not valid for type=LONG: wrongValue", errorMessage);
		
	}
	
	@Test
	public void testRemovePrivateAnnotationsFromBundle() {
		Pair<Annotations, org.sagebionetworks.repo.model.annotation.v2.Annotations> annos = createDummyAnnotations();
		
		subStatus.setSubmissionAnnotations(annos.getSecond());
		
		submissionBundle.setSubmissionStatus(subStatus);
		
		// Call under test
		SubmissionManagerImpl.removePrivateAnnotations(submissionBundle);
		
		assertNotPrivate(subStatus.getAnnotations());
		assertNull(subStatus.getSubmissionAnnotations());
	}
	
	@Test
	public void testRemovePrivateAnnotationsFromStatus() {
		Pair<Annotations, org.sagebionetworks.repo.model.annotation.v2.Annotations> annos = createDummyAnnotations();
		
		subStatus.setSubmissionAnnotations(annos.getSecond());
		
		// Call under test
		SubmissionManagerImpl.removePrivateAnnotations(subStatus);
		
		assertNotPrivate(subStatus.getAnnotations());
		assertNull(subStatus.getSubmissionAnnotations());
	}
	
	private static void assertNotPrivate(Annotations annos) {
		assertNotPrivate(annos.getStringAnnos());
		assertNotPrivate(annos.getLongAnnos());
		assertNotPrivate(annos.getDoubleAnnos());
	}
	
	private static void assertNotPrivate(List<? extends AnnotationBase> annos) {
		annos.forEach( anno -> {
			assertFalse(anno.getIsPrivate());
		});
	}
	
	private static MessageToSend getSubmissionMessage(UserInfo user, String submissionId, ChangeType changeType) {
		return new MessageToSend()
				.withObjectType(ObjectType.SUBMISSION)
				.withChangeType(changeType)
				.withObjectId(submissionId)
				.withUserId(user.getId());
	}

}
