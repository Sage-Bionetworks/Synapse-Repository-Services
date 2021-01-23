package org.sagebionetworks.repo.manager.file.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScannerTestUtils.generateMapping;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.dbo.DDLUtils;
import org.sagebionetworks.repo.model.dbo.DMLUtils;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.ImmutableList;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class BasicFileHandleAssociationScannerAutowireTest {
	
	private static final String TABLE_NAME = "FILE_ASSOCIATION_EXAMPLE";
	private static final String DDL_ID_AND_FILE_HANDLE_ID = "file/ddl_table_with_id_and_file_handle.sql";
	private static final String DDL_ID_AND_CUSTOM_FILE_HANDLE_ID = "file/ddl_table_with_id_and_custom_file_handle.sql";
	private static final String DDL_COMPOSITE_ID_AND_FILE_HANDLE_ID = "file/ddl_table_with_composite_id_and_file_handle.sql";
	
	@Autowired
	private NamedParameterJdbcTemplate jdbcTemplate;
	
	@Autowired
	private DDLUtils ddlUtils;
	
	@BeforeEach
	public void before() {
		ddlUtils.dropTable(TABLE_NAME);
	}
		
	@Test
	public void testGetIdRangeWithIdAndFileHandleId() throws IOException {
		TableMapping<?> tableMapping = generateMapping(TABLE_NAME, DDL_ID_AND_FILE_HANDLE_ID, new FieldColumn[] {
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("fileHandleId", "FILE_HANDLE_ID")
		});
		
		// Uses the default
		String fileHandleIdColumn = null;
		
		testGetIdRange(tableMapping, fileHandleIdColumn,
			ImmutableList.of(
				// ID, FILE_HANDLE_ID
				new Object[] { 2, 1 },
				new Object[] { 4, 1 },
				new Object[] { 10, 2 }
			)
		, new IdRange(2, 10));
	}
	
	@Test
	public void testGetIdRangeWithIdAndCustomFileHandleId() throws IOException {
		TableMapping<?> tableMapping = generateMapping(TABLE_NAME, DDL_ID_AND_CUSTOM_FILE_HANDLE_ID, new FieldColumn[] {
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("fileHandleId", "FILE_ID")
		});
		
		String fileHandleIdColumn = "FILE_ID";
		
		testGetIdRange(tableMapping, fileHandleIdColumn,
			ImmutableList.of(
				// ID, FILE_ID
				new Object[] { 2, 1 },
				new Object[] { 4, 1 },
				new Object[] { 10, 2 }
			)
		, new IdRange(2, 10));
	}
	
	@Test
	public void testGetIdRangeWithCompositeIdAndFileHandleId() throws IOException {
		TableMapping<?> tableMapping = generateMapping(TABLE_NAME, DDL_COMPOSITE_ID_AND_FILE_HANDLE_ID, new FieldColumn[] {
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("version", "VERSION", true),
				new FieldColumn("fileHandleId", "FILE_HANDLE_ID")
		});
		
		String fileHandleIdColumn = "FILE_HANDLE_ID";
		
		testGetIdRange(tableMapping, fileHandleIdColumn,
			ImmutableList.of(
				// ID, VERSION, FILE_HANDLE_ID
				new Object[] { 2, 1, 1 },
				new Object[] { 4, 1, 2 },
				new Object[] { 10, 1, 3 }
			)
		, new IdRange(2, 10));
	}
	
	@Test
	public void testScanRangeWithIdAndFileHandleId() throws IOException {
		TableMapping<?> tableMapping = generateMapping(TABLE_NAME, DDL_ID_AND_FILE_HANDLE_ID, new FieldColumn[] {
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("fileHandleId", "FILE_HANDLE_ID")
		});
		
		// Uses the default
		String fileHandleIdColumn = null;
		long batchSize = 10;
		
		List<ScannedFileHandle> expected = Arrays.asList(
				new ScannedFileHandle("1", 1L),
				new ScannedFileHandle("2", 2L),
				new ScannedFileHandle("5", 1L),
				new ScannedFileHandle("10", 1L)
		);
		
		testScanRange(tableMapping, fileHandleIdColumn, new IdRange(1, 10), batchSize,
			ImmutableList.of(
				// ID, FILE_HANDLE_ID
				new Object[] { 1, 1 },
				new Object[] { 2, 2 },
				new Object[] { 5, 1 },
				new Object[] { 10, 1 }
			)
		, expected);
		
	}
	
	@Test
	public void testScanRangeWithIdAndFileHandleIdAndNullFileHandles() throws IOException {
		TableMapping<?> tableMapping = generateMapping(TABLE_NAME, DDL_ID_AND_FILE_HANDLE_ID, new FieldColumn[] {
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("fileHandleId", "FILE_HANDLE_ID")
		});
		
		// Uses the default
		String fileHandleIdColumn = null;
		long batchSize = 10;
		
		List<ScannedFileHandle> expected = Arrays.asList(
				new ScannedFileHandle("1", 1L),
				new ScannedFileHandle("2", 2L),
				new ScannedFileHandle("10", 1L)
		);
		
		testScanRange(tableMapping, fileHandleIdColumn, new IdRange(1, 10), batchSize,
			ImmutableList.of(
				// ID, FILE_HANDLE_ID
				new Object[] { 1, 1 },
				new Object[] { 2, 2 },
				new Object[] { 5, null },
				new Object[] { 10, 1 }
			)
		, expected);
		
	}
	
	@Test
	public void testScanRangeWithIdAndFileHandleIdWithSubRange() throws IOException {
		TableMapping<?> tableMapping = generateMapping(TABLE_NAME, DDL_ID_AND_FILE_HANDLE_ID, new FieldColumn[] {
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("fileHandleId", "FILE_HANDLE_ID")
		});
		
		// Uses the default
		String fileHandleIdColumn = null;
		long batchSize = 10;
		
		List<ScannedFileHandle> expected = Arrays.asList(
				new ScannedFileHandle("1", 1L),
				new ScannedFileHandle("2", 2L),
				new ScannedFileHandle("5", 1L)
		);
		
		testScanRange(tableMapping, fileHandleIdColumn, new IdRange(1, 5), batchSize,
			ImmutableList.of(
				// ID, FILE_HANDLE_ID
				new Object[] { 1, 1 },
				new Object[] { 2, 2 },
				new Object[] { 5, 1 },
				new Object[] { 10, 1 }
			)
		, expected);
		
	}
	
	@Test
	public void testScanRangeWithIdAndFileHandleIdWithMultipleBatches() throws IOException {
		TableMapping<?> tableMapping = generateMapping(TABLE_NAME, DDL_ID_AND_FILE_HANDLE_ID, new FieldColumn[] {
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("fileHandleId", "FILE_HANDLE_ID")
		});
		
		// Uses the default
		String fileHandleIdColumn = null;
		long batchSize = 5;
		
		List<ScannedFileHandle> expected = Arrays.asList(
				new ScannedFileHandle("1", 1L),
				new ScannedFileHandle("2", 2L),
				new ScannedFileHandle("3", 2L),
				new ScannedFileHandle("4", 3L),
				new ScannedFileHandle("5", 1L),
				new ScannedFileHandle("6", 5L),
				new ScannedFileHandle("10", 1L)
		);
		
		testScanRange(tableMapping, fileHandleIdColumn, new IdRange(1, 10), batchSize,
			ImmutableList.of(
				// ID, FILE_HANDLE_ID
				// First batch
				new Object[] { 1, 1 },
				new Object[] { 2, 2 },
				new Object[] { 3, 2 },
				new Object[] { 4, 3 },
				new Object[] { 5, 1 },
				// Second batch
				new Object[] { 6, 5 },
				new Object[] { 10, 1 }
			)
		, expected);
		
	}
	
	@Test
	public void testScanRangeWithIdAndCompositeId() throws IOException {
		TableMapping<?> tableMapping = generateMapping(TABLE_NAME, DDL_COMPOSITE_ID_AND_FILE_HANDLE_ID, new FieldColumn[] {
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("version", "VERSION", true),
				new FieldColumn("fileHandleId", "FILE_HANDLE_ID")
		});
		
		// Uses the default
		String fileHandleIdColumn = null;
		long batchSize = 5;
		
		List<ScannedFileHandle> expected = Arrays.asList(
				new ScannedFileHandle("1", 1L),
				new ScannedFileHandle("1", 1L),
				new ScannedFileHandle("1", 2L),
				new ScannedFileHandle("4", 3L),
				new ScannedFileHandle("5", 1L),
				new ScannedFileHandle("5", 5L),
				new ScannedFileHandle("10", 1L)
		);
		
		testScanRange(tableMapping, fileHandleIdColumn, new IdRange(1, 10), batchSize,
			ImmutableList.of(
				// ID, VERSION, FILE_HANDLE_ID
				// First batch
				new Object[] { 1, 1, 1 },
				new Object[] { 1, 2, 1 },
				new Object[] { 1, 3, 2 },
				new Object[] { 4, 1, 3 },
				new Object[] { 5, 1, 1 },
				// Second batch
				new Object[] { 5, 2, 5 },
				new Object[] { 10, 1, 1 }
			)
		, expected);
		
	}
	
	@Test
	public void testScanRangeWithIdAndCompositeIdAndNullFileHandles() throws IOException {
		TableMapping<?> tableMapping = generateMapping(TABLE_NAME, DDL_COMPOSITE_ID_AND_FILE_HANDLE_ID, new FieldColumn[] {
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("version", "VERSION", true),
				new FieldColumn("fileHandleId", "FILE_HANDLE_ID")
		});
		
		// Uses the default
		String fileHandleIdColumn = null;
		long batchSize = 5;
		
		List<ScannedFileHandle> expected = Arrays.asList(
				new ScannedFileHandle("1", 1L),
				new ScannedFileHandle("1", 1L),
				new ScannedFileHandle("1", 2L),
				new ScannedFileHandle("5", 5L),
				new ScannedFileHandle("6", 6L),
				new ScannedFileHandle("7", 1L),
				new ScannedFileHandle("7", 7L)
		);
		
		testScanRange(tableMapping, fileHandleIdColumn, new IdRange(1, 10), batchSize,
			ImmutableList.of(
				// ID, VERSION, FILE_HANDLE_ID
				// First batch
				new Object[] { 1, 1, 1 },
				new Object[] { 1, 2, 1 },
				new Object[] { 1, 3, 2 },
				// The following are skipped from the first batch
				new Object[] { 4, 1, null },
				new Object[] { 5, 1, null },
				new Object[] { 5, 2, 5 },
				new Object[] { 6, 1, 6 },
				// Second batch
				new Object[] { 7, 1, 1},
				new Object[] { 7, 2, 7}
			)
		, expected);
		
	}
	
	private void testGetIdRange(TableMapping<?> tableMapping, String fileHandleIdColumn, List<Object[]> data, IdRange expectedRange) throws IOException {
		ddlUtils.validateTableExists(tableMapping);
		
		FileHandleAssociationScanner scanner = getScannerInstance(tableMapping, fileHandleIdColumn);
		
		// Call under test
		IdRange range =  scanner.getIdRange();
		
		assertEquals(new IdRange(-1, -1), range);
		
		// Generate some data
		addData(tableMapping, data);
		
		// Call under test
		range =  scanner.getIdRange();
		
		assertEquals(expectedRange, range);
	}
	
	private void testScanRange(TableMapping<?> tableMapping, String fileHandleIdColumn, IdRange range, long batchSize, List<Object[]> data, List<ScannedFileHandle> expected) throws IOException {
		ddlUtils.validateTableExists(tableMapping);
		
		FileHandleAssociationScanner scanner = getScannerInstance(tableMapping, fileHandleIdColumn);
		
		// Call under test
		List<ScannedFileHandle> result = StreamSupport.stream(scanner.scanRange(range, batchSize).spliterator(), false).collect(Collectors.toList());
		
		assertEquals(Collections.emptyList(), result);
		
		// Generate some data
		addData(tableMapping, data);
		
		// Call under test
		result = StreamSupport.stream(scanner.scanRange(range, batchSize).spliterator(), false).collect(Collectors.toList());
		
		assertEquals(expected, result);
	}
	
	private void addData(TableMapping<?> tableMapping, List<Object[]> data) {
		data.forEach(row -> addData(tableMapping, row));
	}
	
	private void addData(TableMapping<?> tableMapping, Object...values) {
		FieldColumn[] fields = tableMapping.getFieldColumns();
		
		Map<String, Object> params = new HashMap<>();
		
		for (int i=0; i<fields.length; i++) {
			params.put(fields[i].getFieldName(), values[i]);
		}
		
		String sqlInsert = DMLUtils.createInsertStatement(tableMapping);

		jdbcTemplate.update(sqlInsert, params);
	}
	
	private FileHandleAssociationScanner getScannerInstance(TableMapping<?> tableMapping, String fileHandleIdColumn) {
		return fileHandleIdColumn == null ?  new BasicFileHandleAssociationScanner(jdbcTemplate, tableMapping) : new BasicFileHandleAssociationScanner(jdbcTemplate, tableMapping, fileHandleIdColumn);
	}
}
