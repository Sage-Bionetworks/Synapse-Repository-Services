package org.sagebionetworks.repo.manager.storagelocation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.storagelocation.processors.BaseKeyStorageLocationProcessor;
import org.sagebionetworks.repo.manager.storagelocation.processors.BucketOwnerStorageLocationProcessor;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class StorageLocationProcessorAutowireTest {
	
	@Autowired
	private List<StorageLocationProcessor<? extends StorageLocationSetting>> storageLocationProcessors;
	
	@Autowired
	private BaseKeyStorageLocationProcessor baseKeyStorageLocationProcessor;
	
	@Autowired
	private BucketOwnerStorageLocationProcessor bucketOwnerStorageLocationProcessor;
	
	@Test
	public void testOrder() {

		assertFalse(storageLocationProcessors.isEmpty());
		
		// Makes sure the first processor is the base key processor
		assertEquals(baseKeyStorageLocationProcessor, storageLocationProcessors.get(0));
		// Makes sure the last processor is the bucket owner processor
		assertEquals(bucketOwnerStorageLocationProcessor, storageLocationProcessors.get(storageLocationProcessors.size() - 1));
	}

}
