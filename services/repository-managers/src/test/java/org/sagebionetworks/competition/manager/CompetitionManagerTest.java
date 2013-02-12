package org.sagebionetworks.competition.manager;

import static org.mockito.Mockito.*;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.competition.dao.CompetitionDAO;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.util.UserInfoUtils;
import org.sagebionetworks.repo.web.NotFoundException;

public class CompetitionManagerTest {
		
	private static CompetitionManager competitionManager;
	private static Competition comp;
	private static Competition compWithId;
	
	private static IdGenerator mockIdGenerator;
	private static CompetitionDAO mockCompetitionDAO;

	private static final Long OWNER_ID = 123L;
	private static final Long USER_ID = 456L;
	private static UserInfo ownerInfo;
	private static UserInfo userInfo;
	
	private static final String COMPETITION_NAME = "test-competition";
    private static final String COMPETITION_ID = "123";
    private static final String COMPETITION_CONTENT_SOURCE = "Baz";
    private static final String COMPETITION_ETAG = "etag";
    
    @Before
    public void setUp() throws DatastoreException, NotFoundException, InvalidModelException {
    	// ID Generator
    	mockIdGenerator = mock(IdGenerator.class);
    	
    	// Competition DAO
    	mockCompetitionDAO = mock(CompetitionDAO.class);
    	
    	// UserInfo
    	ownerInfo = UserInfoUtils.createValidUserInfo();
    	ownerInfo.getIndividualGroup().setId(OWNER_ID.toString());
    	userInfo = UserInfoUtils.createValidUserInfo();
    	userInfo.getIndividualGroup().setId(USER_ID.toString());
    	
		// Competition
    	Date date = new Date();
		comp = new Competition();
		comp.setName(COMPETITION_NAME);
		comp.setOwnerId(ownerInfo.getIndividualGroup().getId());
        comp.setContentSource(COMPETITION_CONTENT_SOURCE);
        comp.setStatus(CompetitionStatus.PLANNED);
        comp.setCreatedOn(date);
        comp.setEtag(COMPETITION_ETAG);

		compWithId = new Competition();
		compWithId.setName(COMPETITION_NAME);		
		compWithId.setOwnerId(ownerInfo.getIndividualGroup().getId());
		compWithId.setContentSource(COMPETITION_CONTENT_SOURCE);
		compWithId.setStatus(CompetitionStatus.PLANNED);
		compWithId.setCreatedOn(date);
		compWithId.setEtag(COMPETITION_ETAG);
		compWithId.setId(COMPETITION_ID);
        
        // Competition Manager
    	competitionManager = new CompetitionManagerImpl(mockIdGenerator, mockCompetitionDAO);
    	
    	// configure mocks
    	when(mockIdGenerator.generateNewId()).thenReturn(Long.parseLong(COMPETITION_ID));		
    	when(mockCompetitionDAO.get(eq(COMPETITION_ID))).thenReturn(comp);
    	when(mockCompetitionDAO.lookupByName(eq(COMPETITION_NAME))).thenReturn(COMPETITION_ID);
    	when(mockCompetitionDAO.create(eq(compWithId), eq(OWNER_ID))).thenReturn(COMPETITION_ID);
    }
	
	@Test
	public void testCreateCompetition() throws Exception {		
		Competition clone = competitionManager.createCompetition(ownerInfo, comp);
		assertEquals("'create' returned unexpected Competition ID", compWithId, clone);
		verify(mockCompetitionDAO).create(eq(comp), eq(OWNER_ID));
	}
	
	@Test
	public void testCreateCompetitionWithPassedId() throws Exception {
		// Create a Competition with a specified ID, which should get overwritten
		comp.setId(COMPETITION_ID + "foo");
		Competition clone = competitionManager.createCompetition(ownerInfo, comp);
		assertEquals("'create' returned unexpected Competition ID", compWithId, clone);
		verify(mockCompetitionDAO).create(eq(comp), eq(OWNER_ID));
	}
	
	@Test
	public void testGetCompetition() throws DatastoreException, NotFoundException, UnauthorizedException {
		Competition comp2 = competitionManager.getCompetition(COMPETITION_ID);
		assertEquals(comp, comp2);
		verify(mockCompetitionDAO).get(eq(COMPETITION_ID));
	}
	
	@Test
	public void testUpdateCompetitionAsOwner() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		competitionManager.updateCompetition(ownerInfo, compWithId);
		verify(mockCompetitionDAO).update(eq(compWithId));
	}
	
	@Test
	public void testUpdateCompetitionAsUser() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException {
		try {
			competitionManager.updateCompetition(userInfo, compWithId);
			fail("User should not have permission to update competition");
		} catch (UnauthorizedException e) {
			// expected
		}
		verify(mockCompetitionDAO, never()).update(eq(compWithId));
	}

	@Test
	public void testFind() throws DatastoreException, UnauthorizedException, NotFoundException {
		Competition comp2 = competitionManager.findCompetition(COMPETITION_NAME);
		assertEquals(comp, comp2);
		verify(mockCompetitionDAO).lookupByName(eq(COMPETITION_NAME));
	}
	
	@Test(expected=NotFoundException.class)
	public void testFindDoesNotExist() throws DatastoreException, UnauthorizedException, NotFoundException {
		competitionManager.findCompetition(COMPETITION_NAME +  "2");
	}
	
	@Test
	public void testInvalidName() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException {
		// note that the Competition Manager relies on EntityNameValidation.java
		comp.setName("$ This is an invalid name");
		try {
			competitionManager.createCompetition(ownerInfo, comp);			
		} catch (IllegalArgumentException e) {
			// expected
			assertTrue(e.getMessage().toLowerCase().contains("name"));			
		}
		verify(mockCompetitionDAO, times(0)).update(eq(comp));
	}
	
	@Test
	public void testIsAdmin() throws DatastoreException, UnauthorizedException, NotFoundException {
		assertTrue("Owner should be an admin of their own Competition", 
				competitionManager.isCompAdmin(OWNER_ID.toString(), COMPETITION_ID));
		assertFalse("Non-owner user should NOT be an admin of this Competition", 
				competitionManager.isCompAdmin(USER_ID.toString(), COMPETITION_ID));
	}

}
