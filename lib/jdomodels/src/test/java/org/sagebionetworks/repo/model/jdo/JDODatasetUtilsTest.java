package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.jdo.persistence.JDODataset;

public class JDODatasetUtilsTest {
	
	@Test
	public void testRoundTrip() throws InvalidModelException{
		// Create a 
		Dataset toClone = new Dataset();
		toClone.setId("42");
		toClone.setName("someName");
		toClone.setDescription("someDescription");
		toClone.setCreator("someCreator");
		toClone.setCreationDate(new Date(System.currentTimeMillis()));
		toClone.setStatus("someStatus");
		toClone.setReleaseDate(new Date(System.currentTimeMillis()+1001));
//		toClone.setVersion("1.0.1");
		// First go to DTO
		JDODataset jdo = JDODatasetUtils.createFromDTO(toClone);
		assertNotNull(jdo);
		// Now go back
		Dataset copy = JDODatasetUtils.createFromJDO(jdo);
		assertNotNull(copy);
		// The copy should match the original
		assertEquals(toClone, copy);
		
	}

}
