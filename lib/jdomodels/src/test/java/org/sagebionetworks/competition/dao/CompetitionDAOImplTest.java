package org.sagebionetworks.competition.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })

public class CompetitionDAOImplTest {
	
	@Autowired
	private CompetitionDAO competitionDAO;
		
	List<String> toDelete;
	
	private static final String COMPETITION_NAME = "test-competition";
	private static final String COMPETITION_NAME_2 = "test-competition-2";
    private static final String COMPETITION_OWNER_ID = "0";
    private static final String COMPETITION_CONTENT_SOURCE = "Baz";

	@Before
	public void setUp() throws Exception {
		toDelete = new ArrayList<String>();
	}

	@After
	public void tearDown() throws Exception {
		if(toDelete != null && competitionDAO != null){
			for(String id: toDelete){
				try {
					competitionDAO.delete(id);
				} catch (NotFoundException e)  {
					// Already deleted; carry on
				}	
			}
		}
	}
	
	@Test
	public void testCRUD() throws Exception {	
		// Initialize Competition
		Competition comp = new Competition();
		comp.setName(COMPETITION_NAME);
        comp.setContentSource(COMPETITION_CONTENT_SOURCE);
        comp.setStatus(CompetitionStatus.PLANNED);
        
        // Create it
		long initialCount = competitionDAO.getCount();
		String compId = competitionDAO.create(comp, COMPETITION_OWNER_ID);
		assertNotNull(compId);
		toDelete.add(compId);
		
		// Get it
		Competition clone = competitionDAO.get(compId);
		assertEquals(compId, clone.getId());
		assertEquals(COMPETITION_NAME, clone.getName());
		assertEquals(COMPETITION_OWNER_ID, clone.getOwnerId());
		assertEquals(COMPETITION_CONTENT_SOURCE, clone.getContentSource());
		assertEquals(CompetitionStatus.PLANNED, clone.getStatus());
		assertEquals(1 + initialCount, competitionDAO.getCount());
		
		// Update it
		clone.setName(COMPETITION_NAME_2);
		competitionDAO.update(clone);
		Competition updated = competitionDAO.get(compId);
		assertEquals(compId, updated.getId());
		assertFalse("Competition name update failed.", comp.getName().equals(updated.getName()));
		
		// Delete it
		assertNotNull(competitionDAO.get(compId));
		assertNotNull(competitionDAO.lookupByName(updated.getName()));
		competitionDAO.delete(compId);
		try {
			competitionDAO.get(compId);
			fail("found a Competition that should have been deleted");
		} catch (NotFoundException e) {
			// Expected
		}
		assertNull(competitionDAO.lookupByName(updated.getName()));
	}

	@Test
	public void testFind() throws Exception {
		// Initialize Competition
		Competition comp = new Competition();
		comp.setName(COMPETITION_NAME);
        comp.setContentSource(COMPETITION_CONTENT_SOURCE);
        comp.setStatus(CompetitionStatus.PLANNED);
        
        // Create it
		String compId = competitionDAO.create(comp, COMPETITION_OWNER_ID);
		assertNotNull(compId);
		toDelete.add(compId);
		
		// Find it
		assertEquals(compId, competitionDAO.lookupByName(COMPETITION_NAME));		
		assertNull(competitionDAO.lookupByName("" + (new Random()).nextLong()));
	}
	
    @Test
    public void testSameName() throws Exception{
        // Initialize a new competition
		Competition comp = new Competition();
		comp.setName(COMPETITION_NAME);
        comp.setContentSource(COMPETITION_CONTENT_SOURCE);
        comp.setStatus(CompetitionStatus.PLANNED);
 
        // Create it
		long initialCount = competitionDAO.getCount();
		String compId = competitionDAO.create(comp, COMPETITION_OWNER_ID);
		assertNotNull(compId);
		toDelete.add(compId);
		
		// Get it
		Competition clone = competitionDAO.get(compId);
		assertEquals(compId, clone.getId());
		assertEquals(COMPETITION_NAME, clone.getName());
		assertEquals(COMPETITION_OWNER_ID, clone.getOwnerId());
		assertEquals(COMPETITION_CONTENT_SOURCE, clone.getContentSource());
		assertEquals(CompetitionStatus.PLANNED, clone.getStatus());
		assertEquals(1 + initialCount, competitionDAO.getCount());
		
		// Create clone with same name
		clone.setId(compId + 1);		
        try {
        	competitionDAO.create(clone, COMPETITION_OWNER_ID);
        	fail("Should not be able to create two Competitions with the same name");
        } catch (IllegalArgumentException e) {
        	// Expected name conflict
        }
    }
    
    @Test
    public void testDtoToDbo() {
    	//TODO
    }
    
    @Test
    public void testDboToDto() {
    	//TODO
    }

}
