package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.IdRange;
import org.sagebionetworks.repo.manager.file.scanner.ScannedFileHandleAssociation;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
public class AccessRequirementFileHandleAssociationProviderTest {

	@Mock
	private AccessRequirementDAO mockAccessRequirementDao;
	
	@Mock
	private JdbcTemplate mockJdbcTemplate;
	
	@Mock
	private NamedParameterJdbcTemplate mockNamedJdbcTemplate;
	
	@Mock
	private ManagedACTAccessRequirement mockAccessRequirement;
	
	@InjectMocks
	private AccessRequirementFileHandleAssociationProvider provider;
	
	@Test
	public void testGetFileHandleIdsAssociatedWithObjectForNonACTAccessRequirement() {
		String accessRequirementId = "1";
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(new TermsOfUseAccessRequirement());
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(
				Arrays.asList("2"), accessRequirementId);
		assertTrue(associated.isEmpty());
	}
	
	@Test
	public void testGetAssociateType() {
		assertEquals(FileHandleAssociateType.AccessRequirementAttachment, provider.getAssociateType());
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithObjectForACTAccessRequirement() {
		String accessRequirementId = "1";
		String ducTemplateFileHandleId = "2";
		when(mockAccessRequirement.getDucTemplateFileHandleId()).thenReturn(ducTemplateFileHandleId);
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(mockAccessRequirement);
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(
				Arrays.asList(ducTemplateFileHandleId, "3"), accessRequirementId);
		assertEquals(Collections.singleton(ducTemplateFileHandleId), associated);
	}

	@Test
	public void testGetObjectTypeForAssociatedType() {
		assertEquals(ObjectType.ACCESS_REQUIREMENT, provider.getAuthorizationObjectTypeForAssociatedObjectType());
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
		
		verify(mockJdbcTemplate).queryForObject("SELECT MIN(`OWNER_ID`), MAX(`OWNER_ID`) FROM ACCESS_REQUIREMENT_REVISION", null, BasicFileHandleAssociationScanner.ID_RANGE_MAPPER);
		
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
		
		verify(mockNamedJdbcTemplate, times(2)).query(eq("SELECT `OWNER_ID`, `SERIALIZED_ENTITY` FROM ACCESS_REQUIREMENT_REVISION WHERE `OWNER_ID` BETWEEN :BMINID AND :BMAXID AND SERIALIZED_ENTITY IS NOT NULL ORDER BY `OWNER_ID`, `NUMBER` LIMIT :KEY_LIMIT OFFSET :KEY_OFFSET"), anyMap(), any(RowMapper.class));
	}
	
	
}
