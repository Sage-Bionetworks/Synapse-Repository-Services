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
import static org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner.DEFAULT_BATCH_SIZE;
import static org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner.DEFAULT_FILE_ID_COLUMN_NAME;
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
	private RowMapper<ScannedFileHandleAssociation> mockRowMapper;
	
	@Mock
	private RowMapperSupplier mockCustomRowMapperSupplier;
	
	@BeforeEach
	public void before() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testInitScannerWithValidMapping() {
		TableMapping<Object> mapping = generateMapping("SOME_TABLE", 
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("fileHandleId", DEFAULT_FILE_ID_COLUMN_NAME)
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
		new BasicFileHandleAssociationScanner(mockParamaterizedJdbcTemplate, mapping, "FILE_ID", DEFAULT_BATCH_SIZE, null);
	}
	
	@Test
	public void testInitScannerWithValidMappingAndCustomRowMapper() {
		TableMapping<Object> mapping = generateMapping("SOME_TABLE", 
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("fileHandleId", DEFAULT_FILE_ID_COLUMN_NAME)
		);
		
		// Call under test
		new BasicFileHandleAssociationScanner(mockParamaterizedJdbcTemplate, mapping, DEFAULT_FILE_ID_COLUMN_NAME, DEFAULT_BATCH_SIZE, mockCustomRowMapperSupplier);
	}
	
	@Test
	public void testInitScannerWithInvalidBatchSize() {
		TableMapping<Object> mapping = generateMapping("SOME_TABLE", 
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("fileHandleId", DEFAULT_FILE_ID_COLUMN_NAME)
		);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			new BasicFileHandleAssociationScanner(mockParamaterizedJdbcTemplate, mapping, DEFAULT_FILE_ID_COLUMN_NAME, /* batchSize */ 0, null);
		}).getMessage();
		
		assertEquals("The batchSize must be greater than zero.", errorMessage);
		
		errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			new BasicFileHandleAssociationScanner(mockParamaterizedJdbcTemplate, mapping, DEFAULT_FILE_ID_COLUMN_NAME, /* batchSize */ -1, null);
		}).getMessage();
		
		assertEquals("The batchSize must be greater than zero.", errorMessage);
	}
	
	@Test
	public void testInitScannerWithNoTemplate() {
		TableMapping<Object> mapping = generateMapping("SOME_TABLE", 
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("fileHandleId", DEFAULT_FILE_ID_COLUMN_NAME)
		);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			new BasicFileHandleAssociationScanner(/* namedJdbcTemplate */ null, mapping, DEFAULT_FILE_ID_COLUMN_NAME, DEFAULT_BATCH_SIZE, null);
		}).getMessage();
		
		assertEquals("The namedJdbcTemplate is required.", errorMessage);
	}
	
	@Test
	public void testInitScannerWithNoMapping() {
		TableMapping<Object> mapping = null;
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			new BasicFileHandleAssociationScanner(mockParamaterizedJdbcTemplate, mapping, DEFAULT_FILE_ID_COLUMN_NAME, DEFAULT_BATCH_SIZE, null);
		}).getMessage();
		
		assertEquals("The tableMapping is required.", errorMessage);
	}
	
	@Test
	public void testInitScannerWithNoFileHandleColumn() {
		
		TableMapping<Object> mapping = generateMapping("SOME_TABLE", 
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("fileHandleId", DEFAULT_FILE_ID_COLUMN_NAME)
		);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			new BasicFileHandleAssociationScanner(mockParamaterizedJdbcTemplate, mapping, /* fileHandleColumnName */ null, DEFAULT_BATCH_SIZE, null);
		}).getMessage();
		
		assertEquals("The fileHandleColumnName is required.", errorMessage);
	}
	
	@Test
	public void testInitScannerWithNoBackupId() {
		TableMapping<Object> mapping = generateMapping("SOME_TABLE", 
				new FieldColumn("id", "ID", true),
				new FieldColumn("fileHandleId", DEFAULT_FILE_ID_COLUMN_NAME)
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
				new FieldColumn("fileHandleId", DEFAULT_FILE_ID_COLUMN_NAME)
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
				new FieldColumn("fileHandleId", DEFAULT_FILE_ID_COLUMN_NAME)
		);
		
		when(mockParamaterizedJdbcTemplate.query(anyString(), anyMap(), any(RowMapper.class))).thenReturn(
				// No more restuls
				Collections.emptyList()
		);
		
		FileHandleAssociationScanner scanner = new BasicFileHandleAssociationScanner(mockParamaterizedJdbcTemplate, mapping);
		
		IdRange idRange = new IdRange(1, 3);
		
		List<ScannedFileHandleAssociation> result = new ArrayList<>();
		
		// Call under test
		scanner.scanRange(idRange).forEach(result::add);
		
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
				new FieldColumn("fileHandleId", DEFAULT_FILE_ID_COLUMN_NAME)
		);
		
		when(mockParamaterizedJdbcTemplate.query(anyString(), anyMap(), any(RowMapper.class))).thenReturn(
				// No more restuls
				Collections.emptyList()
		);
				
		FileHandleAssociationScanner scanner = new BasicFileHandleAssociationScanner(mockParamaterizedJdbcTemplate, mapping);
		
		IdRange idRange = new IdRange(1, 3);
		
		List<ScannedFileHandleAssociation> result = new ArrayList<>();
		
		// Call under test
		scanner.scanRange(idRange).forEach(result::add);
		
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
				new FieldColumn("fileHandleId", DEFAULT_FILE_ID_COLUMN_NAME)
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
		
		long batchSize = 2;
		
		FileHandleAssociationScanner scanner = new BasicFileHandleAssociationScanner(mockParamaterizedJdbcTemplate, mapping, DEFAULT_FILE_ID_COLUMN_NAME, batchSize, null);
		
		IdRange idRange = new IdRange(1, 3);
		
		List<ScannedFileHandleAssociation> expected = Arrays.asList(
			new ScannedFileHandleAssociation("1", 1L),
			new ScannedFileHandleAssociation("2", 2L),
			new ScannedFileHandleAssociation("3", 3L)
		);
		
		List<ScannedFileHandleAssociation> result = new ArrayList<>();
		
		// Call under test
		scanner.scanRange(idRange).forEach(result::add);
		
		assertEquals(expected, result);
		
		ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
		
		verify(mockParamaterizedJdbcTemplate, times(3)).query(eq("SELECT `ID`, `FILE_HANDLE_ID` FROM SOME_TABLE WHERE `ID` BETWEEN :BMINID AND :BMAXID AND FILE_HANDLE_ID IS NOT NULL ORDER BY `ID` LIMIT :KEY_LIMIT OFFSET :KEY_OFFSET"), paramsCaptor.capture(), any(RowMapper.class));
		
		assertEquals(idRange.getMinId(), paramsCaptor.getAllValues().get(0).get(DMLUtils.BIND_MIN_ID));
		assertEquals(idRange.getMaxId(), paramsCaptor.getAllValues().get(0).get(DMLUtils.BIND_MAX_ID));
	}
	
	@Test
	public void testScanRangeWithCustomRowMapper() {
		TableMapping<Object> mapping = generateMapping("SOME_TABLE", 
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("id", "VERSION", true),
				new FieldColumn("fileHandleId", DEFAULT_FILE_ID_COLUMN_NAME)
		);
		
		when(mockParamaterizedJdbcTemplate.query(anyString(), anyMap(), any(RowMapper.class))).thenReturn(
				// No more restuls
				Collections.emptyList()
		);
		
		when(mockCustomRowMapperSupplier.getRowMapper(any(), any())).thenReturn(mockRowMapper);

		FileHandleAssociationScanner scanner = new BasicFileHandleAssociationScanner(mockParamaterizedJdbcTemplate, mapping, DEFAULT_FILE_ID_COLUMN_NAME, DEFAULT_BATCH_SIZE, mockCustomRowMapperSupplier);
		
		IdRange idRange = new IdRange(1, 3);
		
		List<ScannedFileHandleAssociation> result = new ArrayList<>();
		
		// Call under test
		scanner.scanRange(idRange).forEach(result::add);
		
		assertEquals(Collections.emptyList(), result);

		verify(mockParamaterizedJdbcTemplate).query(eq("SELECT `ID`, `FILE_HANDLE_ID` FROM SOME_TABLE WHERE `ID` BETWEEN :BMINID AND :BMAXID AND FILE_HANDLE_ID IS NOT NULL ORDER BY `ID`, `VERSION` LIMIT :KEY_LIMIT OFFSET :KEY_OFFSET"), anyMap(), eq(mockRowMapper));
		verify(mockCustomRowMapperSupplier).getRowMapper("ID", DEFAULT_FILE_ID_COLUMN_NAME);		
	}
	
	@Test
	public void testScanRangeWithInvalidRange() {
		TableMapping<Object> mapping = generateMapping("SOME_TABLE", 
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("fileHandleId", DEFAULT_FILE_ID_COLUMN_NAME)
		);
		
		FileHandleAssociationScanner scanner = new BasicFileHandleAssociationScanner(mockParamaterizedJdbcTemplate, mapping);
		
		IdRange idRange = new IdRange(3, 1);
		
		// Call under test
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			scanner.scanRange(idRange);
		}).getMessage();
		
		assertEquals("Invalid range, the minId must be lesser or equal than the maxId", message);
		
	}

}
