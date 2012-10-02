package org.sagebionetworks.search;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.repo.model.search.Document;


/**
 * Unit test for the search dao.
 * 
 * @author jmhill
 *
 */
public class SearchDaoImplTest {
	
	@Test
	public void testPrepareDocument(){
		Document doc = new Document();
		doc.setId("123");
		// This should prepare the document to be sent
		SearchDaoImpl.prepareDocument(doc);
		assertNotNull(doc.getFields());
		assertEquals("The document ID must be set in the fields when ",doc.getId(), doc.getFields().getId());
		assertNotNull("A version was not set.",doc.getVersion());
	}

}
