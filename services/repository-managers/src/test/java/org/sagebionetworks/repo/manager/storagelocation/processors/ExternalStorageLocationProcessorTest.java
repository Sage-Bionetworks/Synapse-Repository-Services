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
import org.sagebionetworks.repo.model.project.ExternalStorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;

@ExtendWith(MockitoExtension.class)
public class ExternalStorageLocationProcessorTest {

	private static final String URL = "https://example.com";

	@InjectMocks
	private ExternalStorageLocationProcessor processor;

	@Mock
	private ExternalStorageLocationSetting mockStorageLocation;
	
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
		String url = URL;

		when(mockStorageLocation.getUrl()).thenReturn(url);

		// Call under test
		processor.beforeCreate(mockUserInfo, mockStorageLocation);
	}

	@Test
	public void testBeforeCreateWithNoUrl() {
		String url = null;

		when(mockStorageLocation.getUrl()).thenReturn(url);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			processor.beforeCreate(mockUserInfo, mockStorageLocation);
		});

		assertEquals("The url is required.", ex.getMessage());
	}

	@Test
	public void testBeforeCreateWithInvalidUrl() {
		String url = "invalid url";

		when(mockStorageLocation.getUrl()).thenReturn(url);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			processor.beforeCreate(mockUserInfo, mockStorageLocation);
		});

		assertTrue(ex.getMessage().startsWith("The External URL is not a valid url"));
	}
}
