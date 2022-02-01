package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;

import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class FileEntityFileHandleAssociationProviderTest {

	@Mock
	private NodeDAO mockNodeDao;
	
	
	@InjectMocks
	private FileEntityFileHandleAssociationProvider provider;
	
	@Test
	public void testGetFileHandleIdsAssociatedWithObject() {
		String entityId = "syn1233";
		List<String> fileHandleIds = Arrays.asList("1", "2", "3");
		List<Long> fileHandlIdsLong = fileHandleIds.stream().map(s->Long.parseLong(s)).collect(Collectors.toList());
		Set<String> expected = Sets.newHashSet("1", "2");
		Set<Long> expectedLong = expected.stream().map(s->Long.parseLong(s)).collect(Collectors.toSet());
		
		when(mockNodeDao.getFileHandleIdsAssociatedWithFileEntity(any(), anyLong())).thenReturn(expectedLong);
		
		// Call under test
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(fileHandleIds, entityId);
		
		assertEquals(expected, associated);
		
		verify(mockNodeDao).getFileHandleIdsAssociatedWithFileEntity(fileHandlIdsLong, 1233L);
		
	}

	@Test
	public void testGetObjectTypeForAssociatedType() {
		assertEquals(ObjectType.ENTITY, provider.getAuthorizationObjectTypeForAssociatedObjectType());
	}
	
	@Test
	public void testGetAssociateType() {
		assertEquals(FileHandleAssociateType.FileEntity, provider.getAssociateType());
	}
	
	@Test
	public void testGetFileHandleIdsAssociatedWithFileEntityNullFileHandleIds(){
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			provider.getFileHandleIdsDirectlyAssociatedWithObject(null, "syn123");
		});
		
		assertEquals("fileHandleIds is required.", ex.getMessage());
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithFileEntityNullEntityId(){
		IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			// Call under test
			provider.getFileHandleIdsDirectlyAssociatedWithObject(new ArrayList<String>(), null);
		});
		
		assertEquals("entityId is required.", ex.getMessage());
	}
	
}
