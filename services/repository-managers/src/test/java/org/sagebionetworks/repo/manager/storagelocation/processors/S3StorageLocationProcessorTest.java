package org.sagebionetworks.repo.manager.storagelocation.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;

@ExtendWith(MockitoExtension.class)
public class S3StorageLocationProcessorTest {

	private static final Long USER_ID = 123L;

	private static final String BASE_KEY = "someBaseKey";

	@InjectMocks
	private S3StorageLocationProcessor processor;

	@Mock
	private S3StorageLocationSetting mockStorageLocation;
	
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
		Boolean stsEnabled = false;
		String baseKey = null;

		when(mockStorageLocation.getStsEnabled()).thenReturn(stsEnabled);
		when(mockStorageLocation.getBaseKey()).thenReturn(baseKey);

		// Call under test
		processor.beforeCreate(mockUserInfo, mockStorageLocation);

		verify(mockStorageLocation).setUploadType(UploadType.S3);
		verifyNoMoreInteractions(mockStorageLocation);
	}

	@Test
	public void testBeforeCreateWithBaseKey() throws IOException {
		String baseKey = BASE_KEY;

		when(mockStorageLocation.getBaseKey()).thenReturn(baseKey);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			processor.beforeCreate(mockUserInfo, mockStorageLocation);
		});
		
		assertEquals("Cannot specify baseKey when creating an S3StorageLocationSetting", ex.getMessage());
		
	}

	@Test
	public void testBeforeCreateOverrideUploadType() throws IOException {
		UploadType uploadType = UploadType.GOOGLECLOUDSTORAGE;
		Boolean stsEnabled = false;
		String baseKey = null;

		mockStorageLocation.setUploadType(uploadType);
		when(mockStorageLocation.getStsEnabled()).thenReturn(stsEnabled);
		when(mockStorageLocation.getBaseKey()).thenReturn(baseKey);

		// Call under test
		processor.beforeCreate(mockUserInfo, mockStorageLocation);

		verify(mockStorageLocation).setUploadType(UploadType.S3);
	}

	@Test
	public void testBeforeCreateWithStsEnabledNull() throws IOException {
		Boolean stsEnabled = null;
		String baseKey = null;

		when(mockStorageLocation.getStsEnabled()).thenReturn(stsEnabled);
		when(mockStorageLocation.getBaseKey()).thenReturn(baseKey);

		// Call under test
		processor.beforeCreate(mockUserInfo, mockStorageLocation);

		verify(mockStorageLocation).setUploadType(UploadType.S3);
		verifyNoMoreInteractions(mockStorageLocation);
	}

	@Test
	public void testBeforeCreateWithStsEnabledTrue() throws IOException {
		Boolean stsEnabled = true;
		String baseKey = null;

		when(mockUserInfo.getId()).thenReturn(USER_ID);
		when(mockStorageLocation.getStsEnabled()).thenReturn(stsEnabled);
		when(mockStorageLocation.getBaseKey()).thenReturn(baseKey);

		// Call under test
		processor.beforeCreate(mockUserInfo, mockStorageLocation);

		ArgumentCaptor<String> baseKeyCaptor = ArgumentCaptor.forClass(String.class);

		verify(mockStorageLocation).setUploadType(UploadType.S3);
		verify(mockStorageLocation).setBaseKey(baseKeyCaptor.capture());

		assertTrue(baseKeyCaptor.getValue().startsWith(USER_ID + "/"));
	}
}
