package org.sagebionetworks.competition.manager;

import static org.mockito.Mockito.*;

import java.util.Date;

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.competition.dao.SubmissionDAO;
import org.sagebionetworks.competition.dao.SubmissionStatusDAO;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.competition.model.Submission;
import org.sagebionetworks.competition.model.SubmissionStatus;
import org.sagebionetworks.competition.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.util.UserInfoUtils;
import org.sagebionetworks.repo.web.NotFoundException;

public class SubmissionManagerTest {
		
	private static SubmissionManager submissionManager;	
	private static Competition comp;
	private static Participant part;
	private static Submission sub;
	private static SubmissionStatus subStatus;
	
	private static SubmissionDAO mockSubmissionDAO;
	private static SubmissionStatusDAO mockSubmissionStatusDAO;
	private static CompetitionManager mockCompetitionManager;
	private static ParticipantManager mockParticipantManager;
	
	private static final String COMP_ID = "12";
	private static final String OWNER_ID = "34";
	private static final String USER_ID = "56";
	private static final String SUB_ID = "78";
	private static final String ENTITY_ID = "90";
	
	private static UserInfo ownerInfo;
	private static UserInfo userInfo;
    
    @Before
    public void setUp() throws DatastoreException, NotFoundException, InvalidModelException {    	
		// User Info
    	ownerInfo = UserInfoUtils.createValidUserInfo();
    	ownerInfo.getIndividualGroup().setId(OWNER_ID);
    	userInfo = UserInfoUtils.createValidUserInfo();
    	userInfo.getIndividualGroup().setId(USER_ID);
    	
    	// Objects
		comp = new Competition();
		comp.setName("compName");
		comp.setId(COMP_ID);
		comp.setOwnerId(OWNER_ID);
        comp.setContentSource("contentSource");
        comp.setStatus(CompetitionStatus.OPEN);
        comp.setCreatedOn(new Date());
        comp.setEtag("compEtag");
        
        part = new Participant();
        part.setCompetitionId(COMP_ID);
        part.setCreatedOn(new Date());
        part.setUserId(USER_ID);
        
        sub = new Submission();
        sub.setCompetitionId(COMP_ID);
        sub.setCreatedOn(new Date());
        sub.setEntityId(ENTITY_ID);
        sub.setId(SUB_ID);
        sub.setName("subName");
        sub.setUserId(USER_ID);
        sub.setVersionNumber(0L);
        
        subStatus = new SubmissionStatus();
        subStatus.setEtag("subEtag");
        subStatus.setId(SUB_ID);
        subStatus.setModifiedOn(new Date());
        subStatus.setScore(0L);
        subStatus.setStatus(SubmissionStatusEnum.OPEN);       
		
    	// Mocks
    	mockSubmissionDAO = mock(SubmissionDAO.class);
    	mockSubmissionStatusDAO = mock(SubmissionStatusDAO.class);
    	mockCompetitionManager = mock(CompetitionManager.class);
    	mockParticipantManager = mock(ParticipantManager.class);
    	when(mockParticipantManager.getParticipant(eq(USER_ID), eq(COMP_ID))).thenReturn(part);
    	when(mockCompetitionManager.getCompetition(eq(COMP_ID))).thenReturn(comp);
    	when(mockSubmissionDAO.get(eq(SUB_ID))).thenReturn(sub);
    	when(mockCompetitionManager.isCompAdmin(eq(OWNER_ID), eq(COMP_ID))).thenReturn(true);
    	when(mockSubmissionStatusDAO.get(eq(SUB_ID))).thenReturn(subStatus);
    	
    	// Submission Manager
    	submissionManager = new SubmissionManagerImpl(mockSubmissionDAO, 
    			mockSubmissionStatusDAO, mockCompetitionManager, mockParticipantManager);
    }
	
	@Test
	public void testCRUDAsAdmin() throws Exception {
		submissionManager.createSubmission(userInfo, sub);
		submissionManager.getSubmission(SUB_ID);
		submissionManager.updateSubmissionStatus(ownerInfo, subStatus);
		submissionManager.deleteSubmission(ownerInfo, SUB_ID);
		verify(mockSubmissionDAO).create(eq(sub));
		verify(mockSubmissionDAO, times(3)).get(eq(SUB_ID));
		verify(mockSubmissionDAO).delete(eq(SUB_ID));
		verify(mockSubmissionStatusDAO).create(any(SubmissionStatus.class));
		verify(mockSubmissionStatusDAO).update(any(SubmissionStatus.class));
	}
	
	@Test
	public void testCRUDAsUser() throws NotFoundException {
		submissionManager.createSubmission(userInfo, sub);
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
		verify(mockSubmissionDAO).create(eq(sub));
		verify(mockSubmissionDAO, times(3)).get(eq(SUB_ID));
		verify(mockSubmissionDAO, never()).delete(eq(SUB_ID));
		verify(mockSubmissionStatusDAO).create(any(SubmissionStatus.class));
		verify(mockSubmissionStatusDAO, never()).update(any(SubmissionStatus.class));
	}
	
	@Test
	public void testGetAllSubmissions() throws DatastoreException, UnauthorizedException, NotFoundException {
		SubmissionStatusEnum statusEnum = SubmissionStatusEnum.CLOSED;
		submissionManager.getAllSubmissions(ownerInfo, COMP_ID, null);
		submissionManager.getAllSubmissions(ownerInfo, COMP_ID, statusEnum);
		verify(mockSubmissionDAO).getAllByCompetition(eq(COMP_ID));
		verify(mockSubmissionDAO).getAllByCompetitionAndStatus(eq(COMP_ID), eq(statusEnum));
	}
	
	@Test
	public void testGetAllSubmissionsByUser() throws DatastoreException, NotFoundException {
		submissionManager.getAllSubmissionsByUser(USER_ID);
		verify(mockSubmissionDAO).getAllByUser(eq(USER_ID));
	}
	
	@Test
	public void testGetSubmissionCount() throws DatastoreException, NotFoundException {
		submissionManager.getSubmissionCount(COMP_ID);
		verify(mockSubmissionDAO).getCountByCompetition(eq(COMP_ID));
	}

}
