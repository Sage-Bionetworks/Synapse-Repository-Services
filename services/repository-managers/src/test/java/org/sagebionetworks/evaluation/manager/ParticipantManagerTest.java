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
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.evaluation.EvaluationDAO;
import org.sagebionetworks.repo.model.evaluation.ParticipantDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
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

	private final String evalId = "123";
	private final String ownerId = "456";
	private final String userId = "789";
	
	private UserInfo ownerInfo;
	private UserInfo userInfo;
	
	private static final String COMPETITION_NAME = "test-competition";
    private static final String COMPETITION_CONTENT_SOURCE = KeyFactory.SYN_ROOT_ID;
    private static final String COMPETITION_ETAG = "etag";
    
    @Before
    public void setUp() throws DatastoreException, NotFoundException, InvalidModelException {
		// User Info
    	ownerInfo = new UserInfo(false, ownerId);
    	userInfo = new UserInfo(false, userId);
    	
    	// Competition
		eval = new Evaluation();
		eval.setName(COMPETITION_NAME);
		eval.setId(evalId);
		eval.setOwnerId(ownerId);
        eval.setContentSource(COMPETITION_CONTENT_SOURCE);
        eval.setStatus(EvaluationStatus.OPEN);
        eval.setCreatedOn(new Date());
        eval.setEtag(COMPETITION_ETAG);
        
		// Participant
    	part = new Participant();
		part.setEvaluationId(evalId);
		part.setUserId(userId);
		
    	// Mocks
    	mockParticipantDAO = mock(ParticipantDAO.class);
    	mockEvalDAO = mock(EvaluationDAO.class);
    	when(mockEvalDAO.get(eq(evalId))).thenReturn(eval);
    	mockUserManager = mock(UserManager.class);
    	mockEvaluationManager = mock(EvaluationManager.class);
    	when(mockParticipantDAO.get(eq(userId), eq(evalId))).thenReturn(part);
    	when(mockEvaluationManager.getEvaluation(any(UserInfo.class), eq(evalId))).thenReturn(eval);
    	mockEvalPermissionsManager = mock(EvaluationPermissionsManager.class);
    	when(mockEvalPermissionsManager.hasAccess(any(UserInfo.class), eq(evalId), eq(ACCESS_TYPE.DELETE))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
    	when(mockEvalPermissionsManager.hasAccess(any(UserInfo.class), eq(evalId), eq(ACCESS_TYPE.UPDATE))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
    	when(mockEvalPermissionsManager.hasAccess(any(UserInfo.class), eq(evalId), eq(ACCESS_TYPE.PARTICIPATE))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);

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
    	participantManager.getParticipant(ownerInfo, userId, evalId);
    	participantManager.removeParticipant(ownerInfo, evalId, userId);
    	verify(mockParticipantDAO, times(1)).get(eq(userId), eq(evalId));
    	verify(mockParticipantDAO).delete(eq(userId), eq(evalId));
    }
    
    @Test
    public void testRDAsAdmin_NotOpen() throws NotFoundException {
    	// admin should be able to add participants even if Evaluation is closed
    	eval.setStatus(EvaluationStatus.CLOSED);
    	participantManager.getParticipant(ownerInfo, userId, evalId);
    	participantManager.removeParticipant(ownerInfo, evalId, userId);
    	verify(mockParticipantDAO, times(1)).get(eq(userId), eq(evalId));
    	verify(mockParticipantDAO).delete(eq(userId), eq(evalId));
    }
    
    @Test
    public void testCRDAsUser() throws DatastoreException, NotFoundException {
    	participantManager.addParticipant(userInfo, evalId);
    	participantManager.getParticipant(userInfo, userId, evalId);
    	participantManager.removeParticipant(userInfo, evalId, userId);
    	verify(mockParticipantDAO).create(any(Participant.class));
    	verify(mockParticipantDAO, times(2)).get(eq(userId), eq(evalId));
    	verify(mockParticipantDAO).delete(eq(userId), eq(evalId));
    }
    
    @Test(expected=UnauthorizedException.class)
    public void testCRDAsUser_NotOpen() throws DatastoreException, NotFoundException {
    	// user should not be able to join Evaluation if it is closed
    	eval.setStatus(EvaluationStatus.CLOSED);
    	participantManager.addParticipant(userInfo, evalId);
    }
    
    @Test(expected=UnauthorizedException.class)
    public void testCRDAsUser_NotAbleToParticipate() throws DatastoreException, NotFoundException {
    	when(mockEvalPermissionsManager.hasAccess(any(UserInfo.class), eq(evalId), eq(ACCESS_TYPE.PARTICIPATE))).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
    	participantManager.addParticipant(userInfo, evalId);
    }

    @Test
    public void testGetAllParticipants() throws NumberFormatException, DatastoreException, NotFoundException {
    	participantManager.getAllParticipants(ownerInfo, evalId, 10, 0);
    	verify(mockParticipantDAO).getAllByEvaluation(eq(evalId), eq(10L), eq(0L));
    }
    
    @Test
    public void testGetNumberOfParticipants() throws DatastoreException, NotFoundException {
    	participantManager.getNumberofParticipants(ownerInfo, evalId);
    	verify(mockParticipantDAO).getCountByEvaluation(eq(evalId));
    }
}
