package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
		when(mockProvider.getFileHandleIdsAssociatedWithObject(anyList(), anyString())).thenReturn(sample);
		assertEquals(sample, fileSwitch.getFileHandleIdsAssociatedWithObject(Arrays.asList("1"), "456",
				FileHandleAssociateType.TableEntity));
	}

	@Test
	public void testGetFileHandlePreviewIdsAssociatedWithObject() {
		List<String> fileHandleIds = Arrays.asList("1", "2");
		String associatedObjectId = "456";
		FileHandleAssociateType associationType = FileHandleAssociateType.TableEntity;
		// The input ids are preview associated with the object
		when(mockFileHandleDao.getFileHandleIdsWithPreviewIds(fileHandleIds))
				.thenReturn(ImmutableMap.of("3", "1", "4", "2"));

		when(mockProvider.getFileHandleIdsAssociatedWithObject(anyList(), anyString()))
				.thenReturn(ImmutableSet.of("3", "4"));

		Set<String> results = fileSwitch.getFileHandlePreviewIdsAssociatedWithObject(fileHandleIds, associatedObjectId,
				associationType);

		assertEquals(new HashSet<>(fileHandleIds), results);
	}

	@Test
	public void testGetFileHandlePreviewIdsAssociatedWithObjectWithPartialPreviews() {
		List<String> fileHandleIds = Arrays.asList("1", "2");
		String associatedObjectId = "456";
		FileHandleAssociateType associationType = FileHandleAssociateType.TableEntity;
		// Only the second is a preview associated with the object
		when(mockFileHandleDao.getFileHandleIdsWithPreviewIds(fileHandleIds)).thenReturn(ImmutableMap.of("4", "2"));
		// No file handle is directly associated with the object
		when(mockProvider.getFileHandleIdsAssociatedWithObject(anyList(), anyString()))
				.thenReturn(ImmutableSet.of("4"));

		Set<String> results = fileSwitch.getFileHandlePreviewIdsAssociatedWithObject(fileHandleIds, associatedObjectId,
				associationType);

		assertEquals(Sets.newHashSet("2"), results);
	}

}
