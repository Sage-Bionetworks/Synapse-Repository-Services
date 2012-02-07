package org.sagebionetworks.repo.model.ontology;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * 
 * @author John
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:concept-dao-beans.spb.xml" })
public class ConceptJenaDAOImplTestAutowire {
	
	@Autowired
	ConceptDAO conceptDao;
	
	@Test
	public void testDao() throws DatastoreException, NotFoundException{
		String conceptUri = "http://synapse.sagebase.org/ontology#8994";
		Concept con = conceptDao.getConceptForUri(conceptUri);
		assertNotNull(con);
		System.out.println(con);
		// For now the dao is wired to the wine ontolgoy but it will be replace with the real ontolgoy when we have one.
		List<ConceptSummary> results = conceptDao.getAllConcepts(conceptUri);
		assertNotNull(results);
		assertTrue(results.size() > 3000);
	}

}
