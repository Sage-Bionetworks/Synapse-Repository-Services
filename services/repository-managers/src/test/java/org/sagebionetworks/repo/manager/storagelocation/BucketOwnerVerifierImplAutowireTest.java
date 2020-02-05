package org.sagebionetworks.repo.manager.storagelocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.project.BucketOwnerStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalGoogleCloudStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class BucketOwnerVerifierImplAutowireTest {

	@Autowired
	private BucketOwnerVerifierImpl bucketOwnerVerifier;
	
	@Test
	public void testS3StorageLocationWiring() {
		BucketOwnerStorageLocationSetting storageLocation = new ExternalS3StorageLocationSetting();
		BucketObjectReader reader = bucketOwnerVerifier.getObjectReader(storageLocation);
		assertNotNull(reader);
		assertEquals(ExternalS3StorageLocationSetting.class, reader.getSupportedStorageLocationType());
	}
	
	@Test
	public void testGCStorageLocationWiring() {
		BucketOwnerStorageLocationSetting storageLocation = new ExternalGoogleCloudStorageLocationSetting();
		BucketObjectReader reader = bucketOwnerVerifier.getObjectReader(storageLocation);
		assertNotNull(reader);
		assertEquals(ExternalGoogleCloudStorageLocationSetting.class, reader.getSupportedStorageLocationType());
	}
	
	@Test
	public void testUnsupportedStorageLocation() {
		BucketOwnerStorageLocationSetting storageLocation = Mockito.mock(BucketOwnerStorageLocationSetting.class);
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			bucketOwnerVerifier.getObjectReader(storageLocation);
		});
		assertEquals("Unsupported storage location type: " + storageLocation.getClass().getSimpleName(), e.getMessage());
	}
	
}
