package org.sagebionetworks.evaluation.manager;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.ParticipantDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.util.UserInfoUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

public class ParticipantManagerTest {
		
	private Evaluation eval;
	private ParticipantManager participantManager;
	private Participant part;

	private ParticipantDAO mockParticipantDAO;
	private EvaluationDAO mockEvalDAO;
	private UserManager mockUserManager;
	private EvaluationManager mockEvaluationManager;
	private EvaluationPermissionsManager mockEvalPermissionsManager;

	private static final String EVAL_ID = "123";
	private static final String OWNER_ID = "456";
	private static final String USER_ID = "789";
	
	private UserInfo ownerInfo;
	private UserInfo userInfo;
	
	private static final String COMPETITION_NAME = "test-competition";
    private static final String COMPETITION_CONTENT_SOURCE = KeyFactory.SYN_ROOT_ID;
    private static final String COMPETITION_ETAG = "etag";
    
    @Before
    public void setUp() throws DatastoreException, NotFoundException, InvalidModelException {
		// User Info
    	ownerInfo = UserInfoUtils.createValidUserInfo(false);
    	ownerInfo.getIndividualGroup().setId(OWNER_ID);
    	userInfo = UserInfoUtils.createValidUserInfo(false);
    	userInfo.getIndividualGroup().setId(USER_ID);
    	
    	// Competition
		eval = new Evaluation();
		eval.setName(COMPETITION_NAME);
		eval.setId(EVAL_ID);
		eval.setOwnerId(OWNER_ID);
        eval.setContentSource(COMPETITION_CONTENT_SOURCE);
        eval.setStatus(EvaluationStatus.OPEN);
        eval.setCreatedOn(new Date());
        eval.setEtag(COMPETITION_ETAG);
        
		// Participant
    	part = new Participant();
		part.setEvaluationId(EVAL_ID);
		part.setUserId(USER_ID);
		
    	// Mocks
    	mockParticipantDAO = mock(ParticipantDAO.class);
    	mockEvalDAO = mock(EvaluationDAO.class);
    	when(mockEvalDAO.get(eq(EVAL_ID))).thenReturn(eval);
    	mockUserManager = mock(UserManager.class);
    	mockEvaluationManager = mock(EvaluationManager.class);
    	when(mockParticipantDAO.get(eq(USER_ID), eq(EVAL_ID))).thenReturn(part);
    	when(mockUserManager.getDisplayName(eq(Long.parseLong(USER_ID)))).thenReturn("foo");
    	when(mockEvaluationManager.getEvaluation(any(UserInfo.class), eq(EVAL_ID))).thenReturn(eval);
    	mockEvalPermissionsManager = mock(EvaluationPermissionsManager.class);
    	when(mockEvalPermissionsManager.hasAccess(any(UserInfo.class), eq(EVAL_ID), eq(ACCESS_TYPE.UPDATE))).thenReturn(true);
    	when(mockEvalPermissionsManager.hasAccess(any(UserInfo.class), eq(EVAL_ID), eq(ACCESS_TYPE.PARTICIPATE))).thenReturn(true);

        // Participant Manager
    	participantManager = new ParticipantManagerImpl();
    	ReflectionTestUtils.setField(participantManager, "participantDAO", mockParticipantDAO);
    	ReflectionTestUtils.setField(participantManager, "evaluationDAO", mockEvalDAO);
    	ReflectionTestUtils.setField(participantManager, "evaluationManager", mockEvaluationManager);
    	ReflectionTestUtils.setField(participantManager, "userManager", mockUserManager);
    	ReflectionTestUtils.setField(participantManager, "evaluationPermissionsManager", mockEvalPermissionsManager);
    }

    @Test
    public void testRDAsAdmin() throws NotFoundException {
    	participantManager.getParticipant(OWNER_ID, USER_ID, EVAL_ID);
    	participantManager.removeParticipant(ownerInfo, EVAL_ID, USER_ID);
    	verify(mockParticipantDAO, times(1)).get(eq(USER_ID), eq(EVAL_ID));
    	verify(mockParticipantDAO).delete(eq(USER_ID), eq(EVAL_ID));
    }
    
    @Test
    public void testRDAsAdmin_NotOpen() throws NotFoundException {
    	// admin should be able to add participants even if Evaluation is closed
    	eval.setStatus(EvaluationStatus.CLOSED);
    	participantManager.getParticipant(OWNER_ID, USER_ID, EVAL_ID);
    	participantManager.removeParticipant(ownerInfo, EVAL_ID, USER_ID);
    	verify(mockParticipantDAO, times(1)).get(eq(USER_ID), eq(EVAL_ID));
    	verify(mockParticipantDAO).delete(eq(USER_ID), eq(EVAL_ID));
    }
    
    @Test
    public void testCRDAsUser() throws DatastoreException, NotFoundException {
    	participantManager.addParticipant(userInfo, EVAL_ID);
    	participantManager.getParticipant(USER_ID, USER_ID, EVAL_ID);
    	participantManager.removeParticipant(userInfo, EVAL_ID, USER_ID);
    	verify(mockParticipantDAO).create(any(Participant.class));
    	verify(mockParticipantDAO, times(2)).get(eq(USER_ID), eq(EVAL_ID));
    	verify(mockParticipantDAO).delete(eq(USER_ID), eq(EVAL_ID));
    }
    
    @Test(expected=UnauthorizedException.class)
    public void testCRDAsUser_NotOpen() throws DatastoreException, NotFoundException {
    	// user should not be able to join Evaluation if it is closed
    	eval.setStatus(EvaluationStatus.CLOSED);
    	participantManager.addParticipant(userInfo, EVAL_ID);
    }
    
    @Test(expected=UnauthorizedException.class)
    public void testCRDAsUser_NotAbleToParticipate() throws DatastoreException, NotFoundException {
    	when(mockEvalPermissionsManager.hasAccess(any(UserInfo.class), eq(EVAL_ID), eq(ACCESS_TYPE.PARTICIPATE))).thenReturn(false);
    	participantManager.addParticipant(userInfo, EVAL_ID);
    }

    @Test
    public void testGetAllParticipants() throws NumberFormatException, DatastoreException, NotFoundException {
    	participantManager.getAllParticipants(OWNER_ID, EVAL_ID, 10, 0);
    	verify(mockParticipantDAO).getAllByEvaluation(eq(EVAL_ID), eq(10L), eq(0L));
    }
    
    @Test
    public void testGetNumberOfParticipants() throws DatastoreException, NotFoundException {
    	participantManager.getNumberofParticipants(OWNER_ID, EVAL_ID);
    	verify(mockParticipantDAO).getCountByEvaluation(eq(EVAL_ID));
    }
}
