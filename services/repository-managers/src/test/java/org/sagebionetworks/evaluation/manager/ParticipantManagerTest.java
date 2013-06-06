package org.sagebionetworks.evaluation.manager;

import static org.mockito.Mockito.*;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.evaluation.dao.ParticipantDAO;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationStatus;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.manager.EvaluationManager;
import org.sagebionetworks.evaluation.manager.ParticipantManager;
import org.sagebionetworks.evaluation.manager.ParticipantManagerImpl;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.util.UserInfoUtils;
import org.sagebionetworks.repo.web.NotFoundException;

public class ParticipantManagerTest {
		
	private static Evaluation eval;
	private static ParticipantManager participantManager;
	private static Participant part;
	
	private static ParticipantDAO mockParticipantDAO;
	private static UserManager mockUserManager;
	private static EvaluationManager mockEvaluationManager;
	private static AuthorizationManager mockAuthorizationManager;
	
	private static final String EVAL_ID = "123";
	private static final String OWNER_ID = "456";
	private static final String USER_ID = "789";
	
	private static UserInfo ownerInfo;
	private static UserInfo userInfo;
	
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
    	mockUserManager = mock(UserManager.class);
    	mockEvaluationManager = mock(EvaluationManager.class);
    	when(mockParticipantDAO.get(eq(USER_ID), eq(EVAL_ID))).thenReturn(part);
    	when(mockUserManager.getDisplayName(eq(Long.parseLong(USER_ID)))).thenReturn("foo");
    	when(mockEvaluationManager.getEvaluation(eq(EVAL_ID))).thenReturn(eval);
    	//when(mockEvaluationManager.isEvalAdmin(eq(ownerInfo), eq(EVAL_ID))).thenReturn(true);
    	
    	mockAuthorizationManager = mock(AuthorizationManager.class);
    	when(mockAuthorizationManager.canAccess(eq(userInfo), eq(EVAL_ID), eq(ObjectType.EVALUATION), eq(ACCESS_TYPE.PARTICIPATE))).thenReturn(true);
    	when(mockAuthorizationManager.canAccess(eq(ownerInfo), eq(EVAL_ID), eq(ObjectType.EVALUATION), eq(ACCESS_TYPE.UPDATE))).thenReturn(true);
        
        // Participant Manager
    	participantManager = new ParticipantManagerImpl(mockParticipantDAO, mockUserManager, mockEvaluationManager, mockAuthorizationManager);
    }
	
    @Test
    public void testCRDAsAdmin() throws NotFoundException {
    	participantManager.addParticipantAsAdmin(ownerInfo, EVAL_ID, USER_ID);
    	participantManager.getParticipant(USER_ID, EVAL_ID);
    	participantManager.removeParticipant(ownerInfo, EVAL_ID, USER_ID);
    	verify(mockParticipantDAO).create(any(Participant.class));
    	verify(mockParticipantDAO, times(2)).get(eq(USER_ID), eq(EVAL_ID));
    	verify(mockParticipantDAO).delete(eq(USER_ID), eq(EVAL_ID));
    }
    
    @Test
    public void testCRDAsAdmin_NotOpen() throws NotFoundException {
    	// admin should be able to add participants even if Evaluation is closed
    	eval.setStatus(EvaluationStatus.CLOSED);
    	participantManager.addParticipantAsAdmin(ownerInfo, EVAL_ID, USER_ID);
    	participantManager.getParticipant(USER_ID, EVAL_ID);
    	participantManager.removeParticipant(ownerInfo, EVAL_ID, USER_ID);
    	verify(mockParticipantDAO).create(any(Participant.class));
    	verify(mockParticipantDAO, times(2)).get(eq(USER_ID), eq(EVAL_ID));
    	verify(mockParticipantDAO).delete(eq(USER_ID), eq(EVAL_ID));
    }
    
    @Test
    public void testCRDAsUser() throws DatastoreException, NotFoundException {
    	participantManager.addParticipant(userInfo, EVAL_ID);
    	participantManager.getParticipant(USER_ID, EVAL_ID);
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
    	when(mockAuthorizationManager.canAccess(eq(userInfo), eq(EVAL_ID), eq(ObjectType.EVALUATION), eq(ACCESS_TYPE.PARTICIPATE))).thenReturn(false);    	
    	participantManager.addParticipant(userInfo, EVAL_ID);
    }
    
    
    @Test
    public void testGetAllParticipants() throws NumberFormatException, DatastoreException, NotFoundException {
    	participantManager.getAllParticipants(EVAL_ID, 10, 0);
    	verify(mockParticipantDAO).getAllByEvaluation(eq(EVAL_ID), eq(10L), eq(0L));
    }
    
    @Test
    public void testGetNumberOfParticipants() throws DatastoreException, NotFoundException {
    	participantManager.getNumberofParticipants(EVAL_ID);
    	verify(mockParticipantDAO).getCountByEvaluation(eq(EVAL_ID));
    }

}
