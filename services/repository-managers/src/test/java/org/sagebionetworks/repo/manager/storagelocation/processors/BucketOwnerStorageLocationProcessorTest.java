package org.sagebionetworks.repo.manager.storagelocation.processors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.storagelocation.BucketOwnerVerifier;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.project.BucketOwnerStorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;

@ExtendWith(MockitoExtension.class)
public class BucketOwnerStorageLocationProcessorTest {
	
	@Mock
	private BucketOwnerVerifier mockBucketOwnerVerifier;
	
	@InjectMocks
	private BucketOwnerStorageLocationProcessor processor;
	
	@Mock
	private BucketOwnerStorageLocationSetting mockStorageLocation;
	
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
	public void testBeforeCreate() {
		
		// Call under test
		processor.beforeCreate(mockUserInfo, mockStorageLocation);
		
		verify(mockBucketOwnerVerifier).verifyBucketOwnership(mockUserInfo, mockStorageLocation);
	}
}
