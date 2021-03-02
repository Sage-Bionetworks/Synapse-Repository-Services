package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.collect.ImmutableSet;

@ExtendWith(MockitoExtension.class)
public class FileEntityFileHandleAssociationProviderTest {

	@Mock
	private NodeManager mockNodeManager;
	
	@Mock
	private JdbcTemplate mockJdbcTemplate;
	
	@Mock
	private NamedParameterJdbcTemplate mockNamedJdbcTemplate;
	
	@InjectMocks
	private FileEntityFileHandleAssociationProvider provider;
	
	@Test
	public void testGetFileHandleIdsAssociatedWithObject() {
		String entityId = "syn1233";
		List<String> fileHandleIds = Arrays.asList("1", "2", "3");
		Set<String> expected = ImmutableSet.of("1", "2");
		
		when(mockNodeManager.getFileHandleIdsAssociatedWithFileEntity(any(), any())).thenReturn(expected);
		
		// Call under test
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(fileHandleIds, entityId);
		
		assertEquals(expected, associated);
		
		verify(mockNodeManager).getFileHandleIdsAssociatedWithFileEntity(fileHandleIds, entityId);
		
	}

	@Test
	public void testGetObjectTypeForAssociatedType() {
		assertEquals(ObjectType.ENTITY, provider.getAuthorizationObjectTypeForAssociatedObjectType());
	}
	
	@Test
	public void testGetAssociateType() {
		assertEquals(FileHandleAssociateType.FileEntity, provider.getAssociateType());
	}
	
}
