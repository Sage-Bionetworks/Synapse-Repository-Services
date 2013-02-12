package org.sagebionetworks.competition.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.competition.dbo.CompetitionDBO;
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.CompetitionStatus;
import org.sagebionetworks.repo.model.DatastoreException;
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
    private static final Long COMPETITION_OWNER_ID = 0L;
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
		comp.setId("123");
		comp.setName(COMPETITION_NAME);
        comp.setContentSource(COMPETITION_CONTENT_SOURCE);
        comp.setStatus(CompetitionStatus.PLANNED);
        
        // Create it
		long initialCount = competitionDAO.getCount();
		String compId = competitionDAO.create(comp, COMPETITION_OWNER_ID);
		assertNotNull(compId);
		toDelete.add(compId);
		
		// Get it
		Competition created = competitionDAO.get(compId);
		assertEquals(compId, created.getId());
		assertEquals(COMPETITION_NAME, created.getName());
		assertEquals(COMPETITION_OWNER_ID.toString(), created.getOwnerId());
		assertEquals(COMPETITION_CONTENT_SOURCE, created.getContentSource());
		assertEquals(CompetitionStatus.PLANNED, created.getStatus());
		assertNotNull(created.getEtag());
		assertEquals(1 + initialCount, competitionDAO.getCount());
		
		// Update it
		created.setName(COMPETITION_NAME_2);
		competitionDAO.update(created);
		Competition updated = competitionDAO.get(compId);
		assertEquals(compId, updated.getId());
		assertFalse("Competition name update failed.", comp.getName().equals(updated.getName()));
		assertFalse("eTag was not updated.", created.getEtag().equals(updated.getEtag()));
		
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
		assertEquals(initialCount, competitionDAO.getCount());
		assertNull(competitionDAO.lookupByName(updated.getName()));
	}

	@Test
	public void testFind() throws Exception {
		// Initialize Competition
		Competition comp = new Competition();
		comp.setId("123");
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
		comp.setId("123");
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
		assertEquals(COMPETITION_OWNER_ID.toString(), clone.getOwnerId());
		assertEquals(COMPETITION_CONTENT_SOURCE, clone.getContentSource());
		assertEquals(CompetitionStatus.PLANNED, clone.getStatus());
		assertEquals(1 + initialCount, competitionDAO.getCount());
		
		// Create clone with same name
		clone.setId(compId + 1);
        try {
        	competitionDAO.create(clone, COMPETITION_OWNER_ID);
        	fail("Should not be able to create two Competitions with the same name");
        } catch (DatastoreException e) {
        	// Expected name conflict
        	assertTrue("Name conflict message should contain the requested name", 
        			e.getMessage().contains(COMPETITION_NAME));
        }
    }
    
    @Test
    public void testGetInRange() throws DatastoreException, NotFoundException {
    	// Initialize Competition
		Competition comp = new Competition();
		comp.setId("123");
		comp.setName(COMPETITION_NAME);
        comp.setContentSource(COMPETITION_CONTENT_SOURCE);
        comp.setStatus(CompetitionStatus.PLANNED);
        
        // Create it
		String compId = competitionDAO.create(comp, COMPETITION_OWNER_ID);
		assertNotNull(compId);
		toDelete.add(compId);
		
		// Get it
		comp = competitionDAO.get(compId);
		List<Competition> compList = competitionDAO.getInRange(10, 0);
		assertEquals(1, compList.size());
		assertEquals(comp, compList.get(0));		
    }
    
    @Test
    public void testGetInRangeByStatus() throws DatastoreException, NotFoundException {
    	// Initialize Competition
		Competition comp = new Competition();
		comp.setId("123");
		comp.setName(COMPETITION_NAME);
        comp.setContentSource(COMPETITION_CONTENT_SOURCE);
        comp.setStatus(CompetitionStatus.PLANNED);
        
        // Create it
		String compId = competitionDAO.create(comp, COMPETITION_OWNER_ID);
		assertNotNull(compId);
		toDelete.add(compId);
		
		// Get it
		comp = competitionDAO.get(compId);
		List<Competition> compList = competitionDAO.getInRange(10, 0, CompetitionStatus.PLANNED);
		assertEquals(1, compList.size());
		assertEquals(comp, compList.get(0));
		
		// Verify filtering by status
		compList = competitionDAO.getInRange(10, 0, CompetitionStatus.OPEN);
		assertEquals(0, compList.size());
    }
    
    @Test
    public void testDtoToDbo() {
    	Competition compDTO = new Competition();
    	Competition compDTOclone = new Competition();
    	CompetitionDBO compDBO = new CompetitionDBO();
    	CompetitionDBO compDBOclone = new CompetitionDBO();
    	
    	compDTO.setContentSource("contentSource");
    	compDTO.setCreatedOn(new Date());
    	compDTO.setDescription("description");
    	compDTO.setEtag("eTag");
    	compDTO.setId("123");
    	compDTO.setName("name");
    	compDTO.setOwnerId("456");
    	compDTO.setStatus(CompetitionStatus.OPEN);
    	    	
    	CompetitionDAOImpl.copyDtoToDbo(compDTO, compDBO);
    	CompetitionDAOImpl.copyDboToDto(compDBO, compDTOclone);
    	CompetitionDAOImpl.copyDtoToDbo(compDTOclone, compDBOclone);
    	
    	assertEquals(compDTO, compDTOclone);
    	assertEquals(compDBO, compDBOclone);
    }
}
