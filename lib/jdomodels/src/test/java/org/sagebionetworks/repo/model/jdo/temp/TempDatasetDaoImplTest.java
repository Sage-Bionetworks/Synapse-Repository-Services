package org.sagebionetworks.repo.model.jdo.temp;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodles-test-context.xml" })
public class TempDatasetDaoImplTest {
	
	@Autowired
	TempDatasetDao datasetDao;
	
	// the datasets that must be deleted at the end of each test.
	List<String> toDelete = new ArrayList<String>();
	
	@Before
	public void before(){
		assertNotNull(datasetDao);
		toDelete = new ArrayList<String>();
	}
	
	@After
	public void after(){
		if(toDelete != null && datasetDao != null){
			for(String id:  toDelete){
				// Delete each
				datasetDao.delete(id);
			}
		}
	}
	
	@Test
	public void testCreate() throws DataAccessException, InvalidModelException{
		// Create one
		Dataset newDs = new Dataset();
		newDs.setName("big bad dataset");
		newDs.setDescription("some kind of description");
		String id =  datasetDao.create(newDs);
		assertNotNull(id);
		toDelete.add(id);
		// Make sure we can get it
		Dataset loaded = datasetDao.get(id);
		assertNotNull(loaded);
		assertEquals(id, loaded.getId());		
	}

}
