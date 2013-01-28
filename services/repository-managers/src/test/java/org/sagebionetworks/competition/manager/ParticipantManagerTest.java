package org.sagebionetworks.competition.manager;

import static org.mockito.Mockito.*;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.competition.dao.ParticipantDAO;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.util.UserInfoUtils;
import org.sagebionetworks.repo.web.NotFoundException;

public class ParticipantManagerTest {
		
	private static Competition comp;
	private static ParticipantManager participantManager;
	private static Participant part;
	
	private static ParticipantDAO mockParticipantDAO;
	private static UserManager mockUserManager;
	private static CompetitionManager mockCompetitionManager;
	
	private static final String COMP_ID = "123";
	private static final String OWNER_ID = "456";
	private static final String USER_ID = "789";
	
	private static UserInfo ownerInfo;
	private static UserInfo userInfo;
	
	private static final String COMPETITION_NAME = "test-competition";
    private static final String COMPETITION_CONTENT_SOURCE = "Baz";
    private static final String COMPETITION_ETAG = "etag";
    
    @Before
    public void setUp() throws DatastoreException, NotFoundException, InvalidModelException {
		// User Info
    	ownerInfo = UserInfoUtils.createValidUserInfo();
    	ownerInfo.getIndividualGroup().setId(OWNER_ID);
    	userInfo = UserInfoUtils.createValidUserInfo();
    	userInfo.getIndividualGroup().setId(USER_ID);
    	
    	// Competition
		comp = new Competition();
		comp.setName(COMPETITION_NAME);
		comp.setId(COMP_ID);
		comp.setOwnerId(OWNER_ID);
        comp.setContentSource(COMPETITION_CONTENT_SOURCE);
        comp.setStatus(CompetitionStatus.OPEN);
        comp.setCreatedOn(new Date());
        comp.setEtag(COMPETITION_ETAG);
        
		// Participant
    	part = new Participant();
		part.setCompetitionId(COMP_ID);
		part.setUserId(USER_ID);
		
    	// Mocks
    	mockParticipantDAO = mock(ParticipantDAO.class);
    	mockUserManager = mock(UserManager.class);
    	mockCompetitionManager = mock(CompetitionManager.class);
    	when(mockParticipantDAO.get(eq(USER_ID), eq(COMP_ID))).thenReturn(part);
    	when(mockUserManager.getDisplayName(eq(Long.parseLong(USER_ID)))).thenReturn("foo");
    	when(mockCompetitionManager.getCompetition(eq(COMP_ID))).thenReturn(comp);
    	when(mockCompetitionManager.isCompAdmin(eq(OWNER_ID), eq(COMP_ID))).thenReturn(true);
        
        // Participant Manager
    	participantManager = new ParticipantManagerImpl(mockParticipantDAO, mockUserManager, mockCompetitionManager);
    }
	
    @Test
    public void testCRDAsAdmin() throws NotFoundException {
    	participantManager.addParticipantAsAdmin(ownerInfo, COMP_ID, USER_ID);
    	participantManager.getParticipant(USER_ID, COMP_ID);
    	participantManager.removeParticipant(ownerInfo, COMP_ID, USER_ID);
    	verify(mockParticipantDAO).create(eq(part));
    	verify(mockParticipantDAO, times(2)).get(eq(USER_ID), eq(COMP_ID));
    	verify(mockParticipantDAO).delete(eq(USER_ID), eq(COMP_ID));
    }
    
    @Test
    public void testCRDAsUser() throws DatastoreException, NotFoundException {
    	participantManager.addParticipant(userInfo, COMP_ID);
    	participantManager.getParticipant(USER_ID, COMP_ID);
    	participantManager.removeParticipant(userInfo, COMP_ID, USER_ID);
    	verify(mockParticipantDAO).create(eq(part));
    	verify(mockParticipantDAO, times(2)).get(eq(USER_ID), eq(COMP_ID));
    	verify(mockParticipantDAO).delete(eq(USER_ID), eq(COMP_ID));
    }
    
    @Test
    public void testGetAllParticipants() throws NumberFormatException, DatastoreException, NotFoundException {
    	participantManager.getAllParticipants(COMP_ID, 10, 0);
    	verify(mockParticipantDAO).getAllByCompetition(eq(COMP_ID), eq(10L), eq(0L));
    }
    
    @Test
    public void testGetNumberOfParticipants() throws DatastoreException, NotFoundException {
    	participantManager.getNumberofParticipants(COMP_ID);
    	verify(mockParticipantDAO).getCountByCompetition(eq(COMP_ID));
    }

}
