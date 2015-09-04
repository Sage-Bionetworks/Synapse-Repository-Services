package org.sagebionetworks.repo.manager.file;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationProvider;
import org.sagebionetworks.repo.model.file.FileHandleAssociationType;

import com.google.common.collect.Sets;

public class FileHandleAssociationSwitchImplTest {
	
	FileHandleAssociationProvider mockProvider;
	FileHandleAssociationSwitchImpl fileSwitch;
	
	@Before
	public void before(){
		mockProvider = Mockito.mock(FileHandleAssociationProvider.class);

		
		HashMap<FileHandleAssociationType, FileHandleAssociationProvider> mockMap = new HashMap<FileHandleAssociationType, FileHandleAssociationProvider>();
		mockMap.put(FileHandleAssociationType.TableEntity, mockProvider);
		
		fileSwitch = new FileHandleAssociationSwitchImpl();
		fileSwitch.setProviderMap(mockMap);
	}
	
	@Test
	public void testGetObjectTypeForAssociatedType(){
		when(mockProvider.getObjectTypeForAssociationType()).thenReturn(ObjectType.ENTITY);
		assertEquals(ObjectType.ENTITY, fileSwitch.getObjectTypeForAssociationType(FileHandleAssociationType.TableEntity));
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithObject(){
		Set<String> sample = Sets.newHashSet("1");
		when(mockProvider.getFileHandleIdsAssociatedWithObject(anyList(), anyString())).thenReturn(sample);
		assertEquals(sample, fileSwitch.getFileHandleIdsAssociatedWithObject(Arrays.asList("1"), "456", FileHandleAssociationType.TableEntity));
	}
}
