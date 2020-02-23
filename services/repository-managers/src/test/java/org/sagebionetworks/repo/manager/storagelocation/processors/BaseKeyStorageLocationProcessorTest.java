package org.sagebionetworks.repo.manager.storagelocation.processors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.project.BaseKeyStorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;

@ExtendWith(MockitoExtension.class)
public class BaseKeyStorageLocationProcessorTest {
	
	@InjectMocks
	private BaseKeyStorageLocationProcessor processor;
	
	@Mock
	private BaseKeyStorageLocationSetting mockStorageLocation;
	
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
	public void testCreateBaseKeyStorageLocationSettingWithEmptyBaseKey() {
		String baseKey = "";
		
		when(mockStorageLocation.getBaseKey()).thenReturn(baseKey);		
			
		// Call under test
		processor.beforeCreate(mockUserInfo, mockStorageLocation);
		
		verify(mockStorageLocation).setBaseKey(null);
		
	}
	
	@Test
	public void testCreateBaseKeyStorageLocationSettingWithNullBaseKey() {
		String baseKey = null;
		
		when(mockStorageLocation.getBaseKey()).thenReturn(baseKey);		
		
		// Call under test
		processor.beforeCreate(mockUserInfo, mockStorageLocation);
		
		verify(mockStorageLocation).setBaseKey(null);
		
	}
	
	@Test
	public void testCreateBaseKeyStorageLocationSettingWithBlankBaseKey() {
		String baseKey = "    ";
		
		when(mockStorageLocation.getBaseKey()).thenReturn(baseKey);		
		
		// Call under test
		processor.beforeCreate(mockUserInfo, mockStorageLocation);
		
		verify(mockStorageLocation).setBaseKey(null);
		
	}
	
	@Test
	public void testCreateBaseKeyStorageLocationSettingWithTrailingSlashes() {
		String baseKey = "/someBaseKey/ ";
		
		when(mockStorageLocation.getBaseKey()).thenReturn(baseKey);		
			
		// Call under test
		processor.beforeCreate(mockUserInfo, mockStorageLocation);
		
		verify(mockStorageLocation).setBaseKey("someBaseKey");
		
	}

}
