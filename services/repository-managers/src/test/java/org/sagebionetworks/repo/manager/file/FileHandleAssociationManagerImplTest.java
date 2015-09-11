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
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationProvider;

import com.google.common.collect.Sets;

public class FileHandleAssociationManagerImplTest {
	
	FileHandleAssociationProvider mockProvider;
	FileHandleAssociationManagerImpl fileSwitch;
	
	@Before
	public void before(){
		mockProvider = Mockito.mock(FileHandleAssociationProvider.class);

		
		HashMap<FileHandleAssociateType, FileHandleAssociationProvider> mockMap = new HashMap<FileHandleAssociateType, FileHandleAssociationProvider>();
		mockMap.put(FileHandleAssociateType.TableEntity, mockProvider);
		
		fileSwitch = new FileHandleAssociationManagerImpl();
		fileSwitch.setProviderMap(mockMap);
	}
	
	@Test
	public void testGetObjectTypeForAssociatedType(){
		when(mockProvider.getAuthorizationObjectTypeForAssociatedObjectType()).thenReturn(ObjectType.ENTITY);
		assertEquals(ObjectType.ENTITY, fileSwitch.getAuthorizationObjectTypeForAssociatedObjectType(FileHandleAssociateType.TableEntity));
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithObject(){
		Set<String> sample = Sets.newHashSet("1");
		when(mockProvider.getFileHandleIdsAssociatedWithObject(anyList(), anyString())).thenReturn(sample);
		assertEquals(sample, fileSwitch.getFileHandleIdsAssociatedWithObject(Arrays.asList("1"), "456", FileHandleAssociateType.TableEntity));
	}
}
