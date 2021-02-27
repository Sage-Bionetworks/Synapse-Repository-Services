package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.IdRange;
import org.sagebionetworks.repo.manager.file.scanner.ScannedFileHandleAssociation;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
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
	
	@Test
	public void testScannerIdRange() {
		
		IdRange expected = new IdRange(1, 10);
		
		when(mockNamedJdbcTemplate.getJdbcTemplate()).thenReturn(mockJdbcTemplate);
		when(mockJdbcTemplate.queryForObject(anyString(), any(), eq(BasicFileHandleAssociationScanner.ID_RANGE_MAPPER))).thenReturn(expected);
		
		FileHandleAssociationScanner scanner = provider.getAssociationScanner();
		
		assertNotNull(scanner);
		
		// Call under test
		IdRange range = scanner.getIdRange();
		
		assertEquals(expected, range);
		
		verify(mockJdbcTemplate).queryForObject("SELECT MIN(`OWNER_NODE_ID`), MAX(`OWNER_NODE_ID`) FROM JDOREVISION", null, BasicFileHandleAssociationScanner.ID_RANGE_MAPPER);
		
	}
	
	@Test
	public void testScannerScanRange() {
		
		IdRange range = new IdRange(1, 10);
		
		List<ScannedFileHandleAssociation> expected = Arrays.asList(new ScannedFileHandleAssociation("123", 123L));
		
		when(mockNamedJdbcTemplate.query(any(), anyMap(), any(RowMapper.class))).thenReturn(expected, Collections.emptyList());
				
		FileHandleAssociationScanner scanner = provider.getAssociationScanner();
		
		assertNotNull(scanner);
		
		// Call under test
		List<ScannedFileHandleAssociation> result = StreamSupport.stream(scanner.scanRange(range).spliterator(), false).collect(Collectors.toList());
		
		assertEquals(expected, result);
		
		verify(mockNamedJdbcTemplate, times(2)).query(eq("SELECT `OWNER_NODE_ID`, `FILE_HANDLE_ID` FROM JDOREVISION WHERE `OWNER_NODE_ID` BETWEEN :BMINID AND :BMAXID AND FILE_HANDLE_ID IS NOT NULL ORDER BY `OWNER_NODE_ID`, `NUMBER` LIMIT :KEY_LIMIT OFFSET :KEY_OFFSET"), anyMap(), any(RowMapper.class));
	}
}
