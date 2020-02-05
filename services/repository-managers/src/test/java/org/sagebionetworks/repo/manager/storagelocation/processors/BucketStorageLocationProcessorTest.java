package org.sagebionetworks.repo.manager.storagelocation.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.project.BucketStorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;

@ExtendWith(MockitoExtension.class)
public class BucketStorageLocationProcessorTest {
	
	private static final String BUCKET_NAME = "bucket.name";
	
	@InjectMocks
	private BucketStorageLocationProcessor processor;
	
	@Mock
	private BucketStorageLocationSetting mockStorageLocation;
	
	@Mock
	private StorageLocationSetting mockUnsupportedStorageLocation;
	
	@Mock
	private UserInfo mockUserInfo;
	
	@Test
	public void testSupports() {
		assertTrue(processor.supports(mockStorageLocation.getClass()));
	}
	
	@Test
	public void testSupportsFalse() {
		assertFalse(processor.supports(mockUnsupportedStorageLocation.getClass()));
	}
	
	
	@Test
	public void testBeforeCreateWithValidBucket() {
		String bucketName = BUCKET_NAME;
		
		when(mockStorageLocation.getBucket()).thenReturn(bucketName);
		// Call under test
		processor.beforeCreate(mockUserInfo, mockStorageLocation);
	}
	
	@Test
	public void testBeforeCreateWithNullBucket() {
		String bucketName = null;
		
		when(mockStorageLocation.getBucket()).thenReturn(bucketName);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			processor.beforeCreate(mockUserInfo, mockStorageLocation);
		});
		
		assertEquals("The bucket name is required.", ex.getMessage());
	}
	
	@Test
	public void testBeforeCreateWithBadBucket() {
		String bucketName = "s3://my-bucket-name-is-wrong/";
		
		when(mockStorageLocation.getBucket()).thenReturn(bucketName);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			processor.beforeCreate(mockUserInfo, mockStorageLocation);
		});
		
		assertEquals("Invalid bucket name.", ex.getMessage());
	}
	
	

}
