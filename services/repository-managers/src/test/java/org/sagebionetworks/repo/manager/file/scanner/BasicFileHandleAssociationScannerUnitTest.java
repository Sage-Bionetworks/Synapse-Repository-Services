package org.sagebionetworks.repo.manager.file.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScannerTestUtils.generateMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dbo.DMLUtils;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
public class BasicFileHandleAssociationScannerUnitTest {

	@Mock
	private JdbcTemplate mockJdbcTemplate;
	
	@Mock
	private NamedParameterJdbcTemplate mockParamaterizedJdbcTemplate;
	
	@Mock
	private RowMapper<ScannedFileHandleAssociation> mockCustomRowMapper;
	
	@BeforeEach
	public void before() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testInitScannerWithValidMapping() {
		TableMapping<Object> mapping = generateMapping("SOME_TABLE", 
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("fileHandleId", "FILE_HANDLE_ID")
		);
		
		// Call under test
		new BasicFileHandleAssociationScanner(mockParamaterizedJdbcTemplate, mapping);
	}
	
	@Test
	public void testInitScannerWithValidMappingAndCustomFileHandleColumn() {
		TableMapping<Object> mapping = generateMapping("SOME_TABLE", 
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("fileHandleId", "FILE_ID")
		);
		
		// Call under test
		new BasicFileHandleAssociationScanner(mockParamaterizedJdbcTemplate, mapping, "FILE_ID", null);
	}
	
	@Test
	public void testInitScannerWithNoBackupId() {
		TableMapping<Object> mapping = generateMapping("SOME_TABLE", 
				new FieldColumn("id", "ID", true),
				new FieldColumn("fileHandleId", "FILE_HANDLE_ID")
		);
		
		// Call under test
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			new BasicFileHandleAssociationScanner(mockParamaterizedJdbcTemplate, mapping);
		}).getMessage();
		
		assertEquals("The mapping " + mapping.getClass().getName() + " does not define a backup id column", message);
	}
	
	@Test
	public void testInitScannerWithWrongFileHandleId() {
		TableMapping<Object> mapping = generateMapping("SOME_TABLE", 
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("fileHandleId", "FILE_ID")
		);
		
		// Call under test
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			new BasicFileHandleAssociationScanner(mockParamaterizedJdbcTemplate, mapping);
		}).getMessage();
		
		assertEquals("The column FILE_HANDLE_ID is not defined for mapping " + mapping.getClass().getName(), message);
	}
	
	@Test
	public void testGetIdRange() {
		TableMapping<Object> mapping = generateMapping("SOME_TABLE", 
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("fileHandleId", "FILE_HANDLE_ID")
		);
		
		IdRange expectedRange = new IdRange(1, 2);
		
		when(mockParamaterizedJdbcTemplate.getJdbcTemplate()).thenReturn(mockJdbcTemplate);
		when(mockJdbcTemplate.queryForObject(anyString(), any(), any(RowMapper.class))).thenReturn(expectedRange);
		
		FileHandleAssociationScanner scanner = new BasicFileHandleAssociationScanner(mockParamaterizedJdbcTemplate, mapping);
		
		// Call under test
		IdRange result = scanner.getIdRange();
		
		assertEquals(expectedRange, result);
		
		verify(mockJdbcTemplate).queryForObject(eq("SELECT MIN(`ID`), MAX(`ID`) FROM SOME_TABLE"), eq(null), any(RowMapper.class));
	}
	
	@Test
	public void testScanRangeEmptyResult() {
		TableMapping<Object> mapping = generateMapping("SOME_TABLE", 
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("fileHandleId", "FILE_HANDLE_ID")
		);
		
		when(mockParamaterizedJdbcTemplate.query(anyString(), anyMap(), any(RowMapper.class))).thenReturn(
				// No more restuls
				Collections.emptyList()
		);
		
		FileHandleAssociationScanner scanner = new BasicFileHandleAssociationScanner(mockParamaterizedJdbcTemplate, mapping);
		
		IdRange idRange = new IdRange(1, 3);
		long batchSize = 2;
		
		List<ScannedFileHandleAssociation> result = new ArrayList<>();
		
		// Call under test
		scanner.scanRange(idRange, batchSize).forEach(result::add);
		
		assertEquals(Collections.emptyList(), result);
		
		ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
		
		verify(mockParamaterizedJdbcTemplate).query(eq("SELECT `ID`, `FILE_HANDLE_ID` FROM SOME_TABLE WHERE `ID` BETWEEN :BMINID AND :BMAXID AND FILE_HANDLE_ID IS NOT NULL ORDER BY `ID` LIMIT :KEY_LIMIT OFFSET :KEY_OFFSET"), paramsCaptor.capture(), any(RowMapper.class));
		
		assertEquals(idRange.getMinId(), paramsCaptor.getAllValues().get(0).get(DMLUtils.BIND_MIN_ID));
		assertEquals(idRange.getMaxId(), paramsCaptor.getAllValues().get(0).get(DMLUtils.BIND_MAX_ID));
	}
	
	@Test
	public void testScanRangeWithMultiplePKColumns() {
		TableMapping<Object> mapping = generateMapping("SOME_TABLE", 
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("id", "VERSION", true),
				new FieldColumn("fileHandleId", "FILE_HANDLE_ID")
		);
		
		when(mockParamaterizedJdbcTemplate.query(anyString(), anyMap(), any(RowMapper.class))).thenReturn(
				// No more restuls
				Collections.emptyList()
		);
		
		FileHandleAssociationScanner scanner = new BasicFileHandleAssociationScanner(mockParamaterizedJdbcTemplate, mapping);
		
		IdRange idRange = new IdRange(1, 3);
		long batchSize = 2;
		
		List<ScannedFileHandleAssociation> result = new ArrayList<>();
		
		// Call under test
		scanner.scanRange(idRange, batchSize).forEach(result::add);
		
		assertEquals(Collections.emptyList(), result);
		
		ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
		
		verify(mockParamaterizedJdbcTemplate).query(eq("SELECT `ID`, `FILE_HANDLE_ID` FROM SOME_TABLE WHERE `ID` BETWEEN :BMINID AND :BMAXID AND FILE_HANDLE_ID IS NOT NULL ORDER BY `ID`, `VERSION` LIMIT :KEY_LIMIT OFFSET :KEY_OFFSET"), paramsCaptor.capture(), any(RowMapper.class));
		
		assertEquals(idRange.getMinId(), paramsCaptor.getAllValues().get(0).get(DMLUtils.BIND_MIN_ID));
		assertEquals(idRange.getMaxId(), paramsCaptor.getAllValues().get(0).get(DMLUtils.BIND_MAX_ID));
	}
	
	@Test
	public void testScanRangeWithMultiPage() {
		TableMapping<Object> mapping = generateMapping("SOME_TABLE", 
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("fileHandleId", "FILE_HANDLE_ID")
		);
		
		when(mockParamaterizedJdbcTemplate.query(anyString(), anyMap(), any(RowMapper.class))).thenReturn(
				// First batch
				Arrays.asList(
					new ScannedFileHandleAssociation("1", 1L),
					new ScannedFileHandleAssociation("2", 2L)
				),
				// Second batch
				Arrays.asList(
					new ScannedFileHandleAssociation("3", 3L)
				),
				// No more restuls
				Collections.emptyList()
		);
		
		FileHandleAssociationScanner scanner = new BasicFileHandleAssociationScanner(mockParamaterizedJdbcTemplate, mapping);
		
		IdRange idRange = new IdRange(1, 3);
		long batchSize = 2;
		
		List<ScannedFileHandleAssociation> expected = Arrays.asList(
			new ScannedFileHandleAssociation("1", 1L),
			new ScannedFileHandleAssociation("2", 2L),
			new ScannedFileHandleAssociation("3", 3L)
		);
		
		List<ScannedFileHandleAssociation> result = new ArrayList<>();
		
		// Call under test
		scanner.scanRange(idRange, batchSize).forEach(result::add);
		
		assertEquals(expected, result);
		
		ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
		
		verify(mockParamaterizedJdbcTemplate, times(3)).query(eq("SELECT `ID`, `FILE_HANDLE_ID` FROM SOME_TABLE WHERE `ID` BETWEEN :BMINID AND :BMAXID AND FILE_HANDLE_ID IS NOT NULL ORDER BY `ID` LIMIT :KEY_LIMIT OFFSET :KEY_OFFSET"), paramsCaptor.capture(), any(RowMapper.class));
		
		assertEquals(idRange.getMinId(), paramsCaptor.getAllValues().get(0).get(DMLUtils.BIND_MIN_ID));
		assertEquals(idRange.getMaxId(), paramsCaptor.getAllValues().get(0).get(DMLUtils.BIND_MAX_ID));
	}
	
	@Test
	public void testScaneRangeWithCustomRowMapper() {
		TableMapping<Object> mapping = generateMapping("SOME_TABLE", 
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("id", "VERSION", true),
				new FieldColumn("fileHandleId", "FILE_HANDLE_ID")
		);
		
		when(mockParamaterizedJdbcTemplate.query(anyString(), anyMap(), any(RowMapper.class))).thenReturn(
				// No more restuls
				Collections.emptyList()
		);
		
		FileHandleAssociationScanner scanner = new BasicFileHandleAssociationScanner(mockParamaterizedJdbcTemplate, mapping, "FILE_HANDLE_ID", mockCustomRowMapper);
		
		IdRange idRange = new IdRange(1, 3);
		long batchSize = 2;
		
		List<ScannedFileHandleAssociation> result = new ArrayList<>();
		
		// Call under test
		scanner.scanRange(idRange, batchSize).forEach(result::add);
		
		assertEquals(Collections.emptyList(), result);

		verify(mockParamaterizedJdbcTemplate).query(eq("SELECT `ID`, `FILE_HANDLE_ID` FROM SOME_TABLE WHERE `ID` BETWEEN :BMINID AND :BMAXID AND FILE_HANDLE_ID IS NOT NULL ORDER BY `ID`, `VERSION` LIMIT :KEY_LIMIT OFFSET :KEY_OFFSET"), anyMap(), eq(mockCustomRowMapper));
		
	}
	
	@Test
	public void testScanRangeWithInvalidRange() {
		TableMapping<Object> mapping = generateMapping("SOME_TABLE", 
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("fileHandleId", "FILE_HANDLE_ID")
		);
		
		FileHandleAssociationScanner scanner = new BasicFileHandleAssociationScanner(mockParamaterizedJdbcTemplate, mapping);
		
		IdRange idRange = new IdRange(3, 1);
		long batchSize = 2;
		
		// Call under test
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			scanner.scanRange(idRange, batchSize);
		}).getMessage();
		
		assertEquals("Invalid range, the minId must be lesser or equal than the maxId", message);
		
	}
	
	@Test
	public void testScanRangeWithInvalidBatchSize() {
		TableMapping<Object> mapping = generateMapping("SOME_TABLE", 
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("fileHandleId", "FILE_HANDLE_ID")
		);
		
		FileHandleAssociationScanner scanner = new BasicFileHandleAssociationScanner(mockParamaterizedJdbcTemplate, mapping);
		
		IdRange idRange = new IdRange(1, 3);
		long batchSize = -1;
		
		// Call under test
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			scanner.scanRange(idRange, batchSize);
		}).getMessage();
		
		assertEquals("Invalid batchSize, must be greater than 0", message);
		
	}
	

}
