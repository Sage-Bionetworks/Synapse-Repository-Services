package org.sagebionetworks.competition.manager;

import static org.mockito.Mockito.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.competition.dao.CompetitionDAO;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:services-test-context.xml" })

public class CompetitionManagerTest {
		
	private static CompetitionManager competitionManager;
	private static Competition comp;
	
	private static CompetitionDAO mockCompetitionDAO;
	private static UserInfo mockOwnerInfo;
	private static UserInfo mockUserInfo;
	private static UserGroup ownerGroup;
	private static UserGroup userGroup;
	
	private static final String OWNER_ID = "123";
	private static final String USER_ID = "456";
	
	private static final String COMPETITION_NAME = "test-competition";
    private static final String COMPETITION_ID = "foo";
    private static final String COMPETITION_CONTENT_SOURCE = "Baz";
    
    @Before
    public void setUp() throws DatastoreException, NotFoundException, InvalidModelException {    	
    	// Users
    	ownerGroup = new UserGroup();
    	ownerGroup.setId(OWNER_ID);
    	mockOwnerInfo = mock(UserInfo.class);
    	when(mockOwnerInfo.getIndividualGroup()).thenReturn(ownerGroup);
    	
    	userGroup = new UserGroup();
    	userGroup.setId(USER_ID);
    	mockUserInfo = mock(UserInfo.class);
    	when(mockUserInfo.getIndividualGroup()).thenReturn(userGroup);
    	
    	// Competition DAO
    	mockCompetitionDAO = mock(CompetitionDAO.class);
    	    	
		// Competition
		comp = new Competition();
		comp.setName(COMPETITION_NAME);
		comp.setId(COMPETITION_ID);
		comp.setOwnerId(OWNER_ID);
        comp.setContentSource(COMPETITION_CONTENT_SOURCE);
        comp.setStatus(CompetitionStatus.PLANNED);
        
        // Competition Manger
    	competitionManager = new CompetitionManagerImpl(mockCompetitionDAO);
		when(mockCompetitionDAO.create(eq(comp))).thenReturn(comp.getId());
    	when(mockCompetitionDAO.get(eq(COMPETITION_ID))).thenReturn(comp);
    	when(mockCompetitionDAO.find(eq(COMPETITION_NAME))).thenReturn(comp);
    }
	
	@Test
	public void testCreateCompetition() throws Exception {		
		String compId = competitionManager.createCompetition(mockOwnerInfo, comp);
		assertEquals("'create' returned unexpected Competition ID", comp.getId(), compId);
		verify(mockCompetitionDAO).create(eq(comp));
	}
	
	@Test
	public void testGetCompetition() throws DatastoreException, NotFoundException, UnauthorizedException {
		Competition comp2 = competitionManager.getCompetition(COMPETITION_ID);
		assertEquals(comp, comp2);
		verify(mockCompetitionDAO).get(eq(COMPETITION_ID));
	}
	
	@Test
	public void testUpdateCompetitionAsOwner() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException, UnauthorizedException {
		competitionManager.updateCompetition(mockOwnerInfo, comp);
		verify(mockCompetitionDAO).update(eq(comp));
	}
	
	@Test
	public void testUpdateCompetitionAsUser() throws DatastoreException, InvalidModelException, ConflictingUpdateException, NotFoundException {
		try {
			competitionManager.updateCompetition(mockUserInfo, comp);
			fail("User should not have permission to update competition");
		} catch (UnauthorizedException e) {
			// expected
		}
		verify(mockCompetitionDAO, times(0)).update(eq(comp));
	}

	@Test
	public void testFind() throws DatastoreException, UnauthorizedException, NotFoundException {
		Competition comp2 = competitionManager.findCompetition(COMPETITION_NAME);
		assertEquals(comp, comp2);
		verify(mockCompetitionDAO).find(eq(COMPETITION_NAME));
	}
	
	@Test
	public void testFindDoesNotExist() throws DatastoreException, UnauthorizedException, NotFoundException {
		try {
			Competition comp2 = competitionManager.findCompetition(COMPETITION_NAME +  "2");
			fail("Found a competition that should not exist: " + comp2.toString());
		} catch (NotFoundException e) {
			// expected
		}
	}

}
