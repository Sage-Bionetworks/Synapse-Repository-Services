package org.sagebionetworks.web.util;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.web.shared.Dataset;

/**
 * 
 * @author jmhill
 *
 */
public class RandomDatasetTest {
	
	@Test
	public void testCreateRandom(){
		// Create a random dataset
		Dataset random = RandomDataset.createRandomDataset();
		assertNotNull(random);
		System.out.println(random);
		// Check all fields of the dataset
		assertNotNull(random.getName());
		assertNotNull(random.getCreationDate());
		assertNotNull(random.getReleaseDate());
		assertNotNull(random.getCreator());
		assertNotNull(random.getDescription());
		assertNotNull(random.getId());
		assertNotNull(random.getLayerPreviews());
		assertNotNull(random.getStatus());
		assertNotNull(random.getVersion());
	}

}
