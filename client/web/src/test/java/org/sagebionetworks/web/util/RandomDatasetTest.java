package org.sagebionetworks.web.util;

import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.model.Dataset;

/**
 * 
 * @author jmhill
 *
 */
public class RandomDatasetTest {
	
	@Ignore
	@Test
	public void testCreateRandom(){
		// Create a random dataset
		Dataset random = RandomDataset.createRandomDataset();
		assertNotNull(random);
		System.out.println(random);
		// Check all fields of the dataset
		assertNotNull(random.getName());
		assertNotNull(random.getCreatedOn());
		assertNotNull(random.getReleaseDate());
		assertNotNull(random.getCreatedBy());
		assertNotNull(random.getDescription());
		assertNotNull(random.getId());
//		assertNotNull(random.getLayerPreviews());
		assertNotNull(random.getStatus());
		assertNotNull(random.getVersion());
//		assertNotNull(random.getHasClinicalData());
//		assertNotNull(random.getHasExpressionData());
		assertNotNull(random.getHasGeneticData());
	}

}
