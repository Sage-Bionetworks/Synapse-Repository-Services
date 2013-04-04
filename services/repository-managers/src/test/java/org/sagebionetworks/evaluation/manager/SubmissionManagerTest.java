package org.sagebionetworks.evaluation.manager;

import static org.mockito.Mockito.*;

import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.evaluation.dao.SubmissionStatusDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.manager.EvaluationManager;
import org.sagebionetworks.evaluation.manager.ParticipantManager;
import org.sagebionetworks.evaluation.manager.SubmissionManager;
import org.sagebionetworks.evaluation.manager.SubmissionManagerImpl;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.util.UserInfoUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class SubmissionManagerTest {
		
	private static SubmissionManager submissionManager;	
	private static Evaluation eval;
	private static Participant part;
	private static Submission sub;
	private static Submission sub2;
	private static Submission subWithId;
	private static Submission sub2WithId;
	private static SubmissionStatus subStatus;
	private static EntityBundle bundle;
	
	private static IdGenerator mockIdGenerator;
	private static SubmissionDAO mockSubmissionDAO;
	private static SubmissionStatusDAO mockSubmissionStatusDAO;
	private static EvaluationManager mockCompetitionManager;
	private static ParticipantManager mockParticipantManager;
	
	private static final String EVAL_ID = "12";
	private static final String OWNER_ID = "34";
	private static final String USER_ID = "56";
	private static final String SUB_ID = "78";
	private static final String SUB2_ID = "87";
	private static final String ENTITY_ID = "90";
	private static final String ENTITY2_ID = "99";
	
	private static UserInfo ownerInfo;
	private static UserInfo userInfo;
    
    @Before
    public void setUp() throws DatastoreException, NotFoundException, InvalidModelException {    	
		// User Info
    	ownerInfo = UserInfoUtils.createValidUserInfo(false);
    	ownerInfo.getIndividualGroup().setId(OWNER_ID);
    	userInfo = UserInfoUtils.createValidUserInfo(false);
    	userInfo.getIndividualGroup().setId(USER_ID);
    	
    	// Objects
		eval = new Evaluation();
		eval.setName("compName");
		eval.setId(EVAL_ID);
		eval.setOwnerId(OWNER_ID);
        eval.setContentSource("contentSource");
        eval.setStatus(EvaluationStatus.OPEN);
        eval.setCreatedOn(new Date());
        eval.setEtag("compEtag");
        
        part = new Participant();
        part.setEvaluationId(EVAL_ID);
        part.setCreatedOn(new Date());
        part.setUserId(USER_ID);
        
        sub = new Submission();
        sub.setEvaluationId(EVAL_ID);
        sub.setCreatedOn(new Date());
        sub.setEntityId(ENTITY_ID);
        sub.setName("subName");
        sub.setUserId(USER_ID);
        sub.setVersionNumber(0L);
        
        sub2 = new Submission();
        sub2.setEvaluationId(EVAL_ID);
        sub2.setCreatedOn(new Date());
        sub2.setEntityId(ENTITY2_ID);
        sub2.setName("subName");
        sub2.setUserId(OWNER_ID);
        sub2.setVersionNumber(0L);
        
        subWithId = new Submission();
        subWithId.setId(SUB_ID);
        subWithId.setEvaluationId(EVAL_ID);
        subWithId.setCreatedOn(new Date());
        subWithId.setEntityId(ENTITY_ID);
        subWithId.setName("subName");
        subWithId.setUserId(USER_ID);
        subWithId.setVersionNumber(0L);
        
        sub2WithId = new Submission();
        sub2WithId.setId(SUB2_ID);
        sub2WithId.setEvaluationId(EVAL_ID);
        sub2WithId.setCreatedOn(new Date());
        sub2WithId.setEntityId(ENTITY2_ID);
        sub2WithId.setName("subName");
        sub2WithId.setUserId(OWNER_ID);
        sub2WithId.setVersionNumber(0L);
        
        subStatus = new SubmissionStatus();
        subStatus.setEtag("subEtag");
        subStatus.setId(SUB_ID);
        subStatus.setModifiedOn(new Date());
        subStatus.setScore(0.0);
        subStatus.setStatus(SubmissionStatusEnum.OPEN);
        
        bundle = new EntityBundle();
        bundle.setEntity(new Folder());
		
    	// Mocks
        mockIdGenerator = mock(IdGenerator.class);
    	mockSubmissionDAO = mock(SubmissionDAO.class);
    	mockSubmissionStatusDAO = mock(SubmissionStatusDAO.class);
    	mockCompetitionManager = mock(EvaluationManager.class);
    	mockParticipantManager = mock(ParticipantManager.class);
    	
    	when(mockIdGenerator.generateNewId()).thenReturn(Long.parseLong(SUB_ID));
    	when(mockParticipantManager.getParticipant(eq(USER_ID), eq(EVAL_ID))).thenReturn(part);
    	when(mockCompetitionManager.getEvaluation(eq(EVAL_ID))).thenReturn(eval);
    	when(mockSubmissionDAO.get(eq(SUB_ID))).thenReturn(sub);
    	when(mockCompetitionManager.isEvalAdmin(eq(ownerInfo), eq(EVAL_ID))).thenReturn(true);
    	when(mockSubmissionStatusDAO.get(eq(SUB_ID))).thenReturn(subStatus);
    	
    	// Submission Manager
    	submissionManager = new SubmissionManagerImpl(mockIdGenerator, mockSubmissionDAO, 
    			mockSubmissionStatusDAO, mockCompetitionManager, mockParticipantManager);
    }
	
	@Test
	public void testCRUDAsAdmin() throws Exception {
		assertNull(sub.getId());
		assertNotNull(subWithId.getId());
		submissionManager.createSubmission(userInfo, sub, bundle);
		submissionManager.getSubmission(SUB_ID);
		submissionManager.updateSubmissionStatus(ownerInfo, subStatus);
		submissionManager.deleteSubmission(ownerInfo, SUB_ID);
		verify(mockSubmissionDAO).create(any(Submission.class), any(EntityBundle.class));
		verify(mockSubmissionDAO, times(3)).get(eq(SUB_ID));
		verify(mockSubmissionDAO).delete(eq(SUB_ID));
		verify(mockSubmissionStatusDAO).create(any(SubmissionStatus.class));
		verify(mockSubmissionStatusDAO).update(any(SubmissionStatus.class));
	}
	
	@Test
	public void testCRUDAsUser() throws NotFoundException, DatastoreException, JSONObjectAdapterException {
		assertNull(sub.getId());
		assertNotNull(subWithId.getId());
		submissionManager.createSubmission(userInfo, sub, bundle);
		submissionManager.getSubmission(SUB_ID);
		try {
			submissionManager.updateSubmissionStatus(userInfo, subStatus);
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
		verify(mockSubmissionDAO).create(any(Submission.class), any(EntityBundle.class));
		verify(mockSubmissionDAO, times(3)).get(eq(SUB_ID));
		verify(mockSubmissionDAO, never()).delete(eq(SUB_ID));
		verify(mockSubmissionStatusDAO).create(any(SubmissionStatus.class));
		verify(mockSubmissionStatusDAO, never()).update(any(SubmissionStatus.class));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidScore() throws Exception {
		submissionManager.createSubmission(userInfo, sub, bundle);
		submissionManager.getSubmission(SUB_ID);
		subStatus.setScore(1.1);
		submissionManager.updateSubmissionStatus(ownerInfo, subStatus);
	}
	
	@Test
	public void testGetAllSubmissions() throws DatastoreException, UnauthorizedException, NotFoundException {
		SubmissionStatusEnum statusEnum = SubmissionStatusEnum.CLOSED;
		submissionManager.getAllSubmissions(ownerInfo, EVAL_ID, null, 10, 0);
		submissionManager.getAllSubmissions(ownerInfo, EVAL_ID, statusEnum, 10, 0);
		verify(mockSubmissionDAO).getAllByEvaluation(eq(EVAL_ID), anyLong(), anyLong());
		verify(mockSubmissionDAO).getAllByEvaluationAndStatus(eq(EVAL_ID), eq(statusEnum), eq(10L), eq(0L));
	}
	
	@Test(expected = UnauthorizedException.class)
	public void testGetAllSubmissionsUnauthorized() throws DatastoreException, UnauthorizedException, NotFoundException {
		submissionManager.getAllSubmissions(ownerInfo, USER_ID, null, 10, 0);
	}
	
	@Test
	public void testGetAllSubmissionsByUser() throws DatastoreException, NotFoundException {
		submissionManager.getAllSubmissionsByUser(USER_ID, 10, 0);
		verify(mockSubmissionDAO).getAllByUser(eq(USER_ID), eq(10L), eq(0L));
	}
	
	@Test
	public void testGetSubmissionCount() throws DatastoreException, NotFoundException {
		submissionManager.getSubmissionCount(EVAL_ID);
		verify(mockSubmissionDAO).getCountByEvaluation(eq(EVAL_ID));
	}

}
