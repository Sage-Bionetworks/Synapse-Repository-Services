package org.sagebionetworks.repo.manager.storagelocation.processors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalGoogleCloudStorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;

@ExtendWith(MockitoExtension.class)
public class ExternalGCStorageLocationProcessorTest {

	@InjectMocks
	private ExternalGoogleCloudStorageLocationProcessor processor;

	@Mock
	private ExternalGoogleCloudStorageLocationSetting mockStorageLocation;
	
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
	public void testBeforeCreate() throws IOException {

		// Call under test
		processor.beforeCreate(mockUserInfo, mockStorageLocation);

		verify(mockStorageLocation).setUploadType(UploadType.GOOGLECLOUDSTORAGE);
	}
}
