package org.sagebionetworks.repo.model.jdo.temp;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.jdo.persistence.JDOAnnotations;
import org.sagebionetworks.repo.model.jdo.persistence.JDODataset;
import org.sagebionetworks.repo.model.jdo.persistence.JDOStringAnnotation;
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
		JDODataset newDs = new JDODataset();
		newDs.setName("big bad dataset");
		newDs.setDescription("some kind of description");
		String id =  datasetDao.create(newDs);
		assertNotNull(id);
		toDelete.add(id);
		// Make sure we can get it
		JDODataset loaded = datasetDao.get(id);
		assertNotNull(loaded);
		assertEquals(id, loaded.getId().toString());
		assertEquals(newDs.getName(), loaded.getName());
		assertEquals(newDs.getDescription(), loaded.getDescription());
	}
	
	@Test
	public void testAnnotations() throws DataAccessException, InvalidModelException{
		// Create one
		JDODataset newDs = new JDODataset();
		newDs.setName("TempDatasetDaoImplTest-testAnnotations");
		newDs.setDescription("some kind of description");
		String id =  datasetDao.create(newDs);
		assertNotNull(id);
		toDelete.add(id);
		// Now edit the annotations
		JDODataset loaded = datasetDao.get(id);
		assertNotNull(loaded);
		JDOAnnotations annos = datasetDao.getAnnotations(JDODataset.class, id);
		assertNotNull(annos);
		annos.add("stringOne", "one");
		annos.add("longOne", new Long(101));
		// now upated
		datasetDao.updateAnnotations(JDODataset.class, id, annos);
		// Make sure the values are there
		annos = datasetDao.getAnnotations(JDODataset.class, id);
		assertNotNull(annos);
		Set<JDOStringAnnotation> stringSet = annos.getStringAnnotations();
		assertNotNull(stringSet);
		assertEquals(1, stringSet.size());
		JDOStringAnnotation stringAnno = stringSet.iterator().next();
		assertNotNull(stringAnno);
		assertEquals("stringOne", stringAnno.getAttribute());
		assertEquals("one", stringAnno.getValue());

	}

}
