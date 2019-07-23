package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationProvider;

import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class FileHandleAssociationManagerImplTest {

	@Mock
	FileHandleAssociationProvider mockProvider;

	@Mock
	FileHandleDao mockFileHandleDao;

	FileHandleAssociationManagerImpl fileSwitch;

	@BeforeEach
	public void before() {

		HashMap<FileHandleAssociateType, FileHandleAssociationProvider> mockMap = new HashMap<FileHandleAssociateType, FileHandleAssociationProvider>();
		mockMap.put(FileHandleAssociateType.TableEntity, mockProvider);

		fileSwitch = new FileHandleAssociationManagerImpl(mockFileHandleDao);
		fileSwitch.setProviderMap(mockMap);
	}

	@Test
	public void testGetObjectTypeForAssociatedType() {
		when(mockProvider.getAuthorizationObjectTypeForAssociatedObjectType()).thenReturn(ObjectType.ENTITY);
		assertEquals(ObjectType.ENTITY,
				fileSwitch.getAuthorizationObjectTypeForAssociatedObjectType(FileHandleAssociateType.TableEntity));
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithObject() {
		Set<String> sample = Sets.newHashSet("1");
		when(mockProvider.getFileHandleIdsDirectlyAssociatedWithObject(anyList(), anyString())).thenReturn(sample);
		assertEquals(sample, fileSwitch.getFileHandleIdsAssociatedWithObject(Arrays.asList("1"), "456",
				FileHandleAssociateType.TableEntity));
	}

}
