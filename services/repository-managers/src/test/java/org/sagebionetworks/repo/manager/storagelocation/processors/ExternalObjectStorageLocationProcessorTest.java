package org.sagebionetworks.repo.manager.storagelocation.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.project.ExternalObjectStorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;

@ExtendWith(MockitoExtension.class)
public class ExternalObjectStorageLocationProcessorTest {

	private static final String ENDPOINT_URL = "https://myendpoint.com";

	@InjectMocks
	private ExternalObjectStorageLocationProcessor processor;

	@Mock
	private ExternalObjectStorageLocationSetting mockStorageLocation;
	
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
		String endpointUrl = ENDPOINT_URL;

		when(mockStorageLocation.getEndpointUrl()).thenReturn(endpointUrl);

		// Call under test
		processor.beforeCreate(mockUserInfo, mockStorageLocation);
	}

	@Test
	public void testBeforeCreateWithTrailingSlashes() {
		String endpointUrl = "/" + ENDPOINT_URL + "/ ";

		when(mockStorageLocation.getEndpointUrl()).thenReturn(endpointUrl);

		// Call under test
		processor.beforeCreate(mockUserInfo, mockStorageLocation);

		ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

		verify(mockStorageLocation).setEndpointUrl(captor.capture());
		assertEquals(ENDPOINT_URL, captor.getValue());
	}

	@Test
	public void testBeforeCreateWithInvalidUrl() {
		String endpointUrl = "invalid url";

		when(mockStorageLocation.getEndpointUrl()).thenReturn(endpointUrl);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			processor.beforeCreate(mockUserInfo, mockStorageLocation);
		});

		assertTrue(ex.getMessage().startsWith("The External URL is not a valid url"));
	}

}
