package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ACCESS_REQUIREMENT_REVISION_SERIALIZED_ENTITY;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.SelfSignAccessRequirement;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.dbo.dao.AccessRequirementUtils;
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
	
	@Mock
	private ResultSet mockResultSet;
	
	@Mock
	private Blob mockBlob;
	
	@InjectMocks
	private AccessRequirementFileHandleAssociationProvider provider;
	
	private RowMapper<ScannedFileHandleAssociation> scannedMapper = AccessRequirementFileHandleAssociationProvider.SCANNED_MAPPER;

	@Test
	public void testGetFileHandleIdsAssociatedWithObjectForNonACTAccessRequirement() {
		String accessRequirementId = "1";
		when(mockAccessRequirementDao.get(accessRequirementId)).thenReturn(new TermsOfUseAccessRequirement());
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(
				Arrays.asList("2"), accessRequirementId);
		assertTrue(associated.isEmpty());
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
		
		when(mockNamedJdbcTemplate.query(any(), anyMap(), eq(scannedMapper))).thenReturn(expected, Collections.emptyList());
				
		FileHandleAssociationScanner scanner = provider.getAssociationScanner();
		
		assertNotNull(scanner);
		
		// Call under test
		List<ScannedFileHandleAssociation> result = StreamSupport.stream(scanner.scanRange(range).spliterator(), false).collect(Collectors.toList());
		
		assertEquals(expected, result);
		
		verify(mockNamedJdbcTemplate, times(2)).query(eq("SELECT `OWNER_ID`, `SERIALIZED_ENTITY` FROM ACCESS_REQUIREMENT_REVISION WHERE `OWNER_ID` BETWEEN :BMINID AND :BMAXID AND SERIALIZED_ENTITY IS NOT NULL ORDER BY `OWNER_ID`, `NUMBER` LIMIT :KEY_LIMIT OFFSET :KEY_OFFSET"), anyMap(), eq(scannedMapper));
	}
	
	@Test
	public void testScannerMapperWithNoSerializedField() throws SQLException {
		
		String objectId = "123";
		
		when(mockResultSet.getString(COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID)).thenReturn("123");
		when(mockResultSet.getBlob(COL_ACCESS_REQUIREMENT_REVISION_SERIALIZED_ENTITY)).thenReturn(null);
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(objectId);
		
		// Call under test
		ScannedFileHandleAssociation result = scannedMapper.mapRow(mockResultSet, 0);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testScannerMapperWithMalformedSerializedField() throws SQLException {
		
		String objectId = "123";
		
		when(mockResultSet.getString(COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID)).thenReturn("123");
		when(mockResultSet.getBlob(COL_ACCESS_REQUIREMENT_REVISION_SERIALIZED_ENTITY)).thenReturn(mockBlob);
		when(mockBlob.getBytes(anyLong(), anyInt())).thenReturn(new byte[] {});
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(objectId);
		
		// Call under test
		ScannedFileHandleAssociation result = scannedMapper.mapRow(mockResultSet, 0);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testScannerMapperWithNotManagedAR() throws SQLException {
		
		AccessRequirement ar = new SelfSignAccessRequirement();
		
		String objectId = "123";
		
		when(mockResultSet.getString(COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID)).thenReturn("123");
		when(mockResultSet.getBlob(COL_ACCESS_REQUIREMENT_REVISION_SERIALIZED_ENTITY)).thenReturn(mockBlob);
		when(mockBlob.getBytes(anyLong(), anyInt())).thenReturn(AccessRequirementUtils.writeSerializedField(ar));
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(objectId);
		
		// Call under test
		ScannedFileHandleAssociation result = scannedMapper.mapRow(mockResultSet, 0);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testScannerMapperWithManagedARAndNoFileHandle() throws SQLException {
		
		AccessRequirement ar = new ManagedACTAccessRequirement();
		
		String objectId = "123";
		
		when(mockResultSet.getString(COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID)).thenReturn("123");
		when(mockResultSet.getBlob(COL_ACCESS_REQUIREMENT_REVISION_SERIALIZED_ENTITY)).thenReturn(mockBlob);
		when(mockBlob.getBytes(anyLong(), anyInt())).thenReturn(AccessRequirementUtils.writeSerializedField(ar));
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(objectId);
		
		// Call under test
		ScannedFileHandleAssociation result = scannedMapper.mapRow(mockResultSet, 0);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testScannerMapperWithManagedARAndMalformedFileHandle() throws SQLException {
		
		ManagedACTAccessRequirement ar = new ManagedACTAccessRequirement();
		ar.setDucTemplateFileHandleId("malformed");
		
		String objectId = "123";
		
		when(mockResultSet.getString(COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID)).thenReturn("123");
		when(mockResultSet.getBlob(COL_ACCESS_REQUIREMENT_REVISION_SERIALIZED_ENTITY)).thenReturn(mockBlob);
		when(mockBlob.getBytes(anyLong(), anyInt())).thenReturn(AccessRequirementUtils.writeSerializedField(ar));
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(objectId);
		
		// Call under test
		ScannedFileHandleAssociation result = scannedMapper.mapRow(mockResultSet, 0);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testScannerMapperWithManagedARAndFileHandle() throws SQLException {
		
		ManagedACTAccessRequirement ar = new ManagedACTAccessRequirement();
		ar.setDucTemplateFileHandleId("456");
		
		String objectId = "123";
		
		when(mockResultSet.getString(COL_ACCESS_REQUIREMENT_REVISION_OWNER_ID)).thenReturn("123");
		when(mockResultSet.getBlob(COL_ACCESS_REQUIREMENT_REVISION_SERIALIZED_ENTITY)).thenReturn(mockBlob);
		when(mockBlob.getBytes(anyLong(), anyInt())).thenReturn(AccessRequirementUtils.writeSerializedField(ar));
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(objectId, 456L);
		
		// Call under test
		ScannedFileHandleAssociation result = scannedMapper.mapRow(mockResultSet, 0);
		
		assertEquals(expected, result);
	}
	
}
