package org.sagebionetworks.repo.manager.file.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner.DEFAULT_BATCH_SIZE;
import static org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScannerTestUtils.generateMapping;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dbo.DDLUtils;
import org.sagebionetworks.repo.model.dbo.DMLUtils;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.file.IdRange;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class BasicFileHandleAssociationScannerAutowireTest {
	
	private static final String TABLE_NAME = "FILE_ASSOCIATION_EXAMPLE";
	private static final String FILE_ID_COLUMN_NAME = "FILE_HANDLE_ID";
	private static final String DDL_ID_AND_FILE_HANDLE_ID = "file/ddl_table_with_id_and_file_handle.sql";
	private static final String DDL_COMPOSITE_ID_AND_FILE_HANDLE_ID = "file/ddl_table_with_composite_id_and_file_handle.sql";
	private static final String DDL_BLOB_FILE_HANDLE = "file/ddl_table_with_blob_file_handle.sql";
	
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
				new FieldColumn("fileHandleId", FILE_ID_COLUMN_NAME).withHasFileHandleRef(true)
		});
		
		testGetIdRange(tableMapping,
			ImmutableList.of(
				// ID, FILE_HANDLE_ID
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
				new FieldColumn("fileHandleId", FILE_ID_COLUMN_NAME).withHasFileHandleRef(true)
		});
		
		testGetIdRange(tableMapping,
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
				new FieldColumn("fileHandleId", FILE_ID_COLUMN_NAME).withHasFileHandleRef(true)
		});
		
		long batchSize = 10;
		
		List<ScannedFileHandleAssociation> expected = Arrays.asList(
				new ScannedFileHandleAssociation(1L, 1L),
				new ScannedFileHandleAssociation(2L, 2L),
				new ScannedFileHandleAssociation(5L, 1L),
				new ScannedFileHandleAssociation(10L, 1L)
		);
		
		testScanRange(tableMapping, new IdRange(1, 10), batchSize,
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
				new FieldColumn("fileHandleId", FILE_ID_COLUMN_NAME).withHasFileHandleRef(true)
		});
		
		long batchSize = 10;
		
		List<ScannedFileHandleAssociation> expected = Arrays.asList(
				new ScannedFileHandleAssociation(1L, 1L),
				new ScannedFileHandleAssociation(2L, 2L),
				new ScannedFileHandleAssociation(10L, 1L)
		);
		
		testScanRange(tableMapping, new IdRange(1, 10), batchSize,
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
				new FieldColumn("fileHandleId", FILE_ID_COLUMN_NAME).withHasFileHandleRef(true)
		});
		
		long batchSize = 10;
		
		List<ScannedFileHandleAssociation> expected = Arrays.asList(
				new ScannedFileHandleAssociation(1L, 1L),
				new ScannedFileHandleAssociation(2L, 2L),
				new ScannedFileHandleAssociation(5L, 1L)
		);
		
		testScanRange(tableMapping, new IdRange(1, 5), batchSize,
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
				new FieldColumn("fileHandleId", FILE_ID_COLUMN_NAME).withHasFileHandleRef(true)
		});
		
		long batchSize = 5;
		
		List<ScannedFileHandleAssociation> expected = Arrays.asList(
				new ScannedFileHandleAssociation(1L, 1L),
				new ScannedFileHandleAssociation(2L, 2L),
				new ScannedFileHandleAssociation(3L, 2L),
				new ScannedFileHandleAssociation(4L, 3L),
				new ScannedFileHandleAssociation(5L, 1L),
				new ScannedFileHandleAssociation(6L, 5L),
				new ScannedFileHandleAssociation(10L, 1L)
		);
		
		testScanRange(tableMapping, new IdRange(1, 10), batchSize,
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
	public void testScanRangeWithIdAndFileHandleIdWithMultipleBatchesAndSkipPages() throws IOException {
		TableMapping<?> tableMapping = generateMapping(TABLE_NAME, DDL_ID_AND_FILE_HANDLE_ID, new FieldColumn[] {
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("fileHandleId", FILE_ID_COLUMN_NAME).withHasFileHandleRef(true)
		});
		
		long batchSize = 2;
		
		List<ScannedFileHandleAssociation> expected = Arrays.asList(
				new ScannedFileHandleAssociation(1L, 1L),
				new ScannedFileHandleAssociation(2L, 2L),
				new ScannedFileHandleAssociation(6L, 5L),
				new ScannedFileHandleAssociation(10L, 1L),
				new ScannedFileHandleAssociation(11L, 6L)
		);
		
		testScanRange(tableMapping, new IdRange(1, 11), batchSize,
			ImmutableList.of(
				// ID, FILE_HANDLE_ID
				// First batch
				new Object[] { 1, 1 },
				new Object[] { 2, 2 },
				// The whole page should be skipped as they are all null
				new Object[] { 3, null },
				new Object[] { 4, null },
				// Second batch, should still be visited
				new Object[] { 6, 5 },
				new Object[] { 10, 1 },
				// Third half empty batch
				new Object[] { 11, 6 }
			)
		, expected);
		
	}
	
	@Test
	public void testScanRangeWithIdAndCompositeId() throws IOException {
		TableMapping<?> tableMapping = generateMapping(TABLE_NAME, DDL_COMPOSITE_ID_AND_FILE_HANDLE_ID, new FieldColumn[] {
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("version", "VERSION", true),
				new FieldColumn("fileHandleId", FILE_ID_COLUMN_NAME).withHasFileHandleRef(true)
		});
		
		long batchSize = 5;
		
		List<ScannedFileHandleAssociation> expected = Arrays.asList(
				new ScannedFileHandleAssociation(1L, 1L),
				new ScannedFileHandleAssociation(1L, 1L),
				new ScannedFileHandleAssociation(1L, 2L),
				new ScannedFileHandleAssociation(4L, 3L),
				new ScannedFileHandleAssociation(5L, 1L),
				new ScannedFileHandleAssociation(5L, 5L),
				new ScannedFileHandleAssociation(10L, 1L)
		);
		
		testScanRange(tableMapping, new IdRange(1, 10), batchSize,
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
				new FieldColumn("fileHandleId", FILE_ID_COLUMN_NAME).withHasFileHandleRef(true)
		});
		
		long batchSize = 5;
		
		List<ScannedFileHandleAssociation> expected = Arrays.asList(
				new ScannedFileHandleAssociation(1L, 1L),
				new ScannedFileHandleAssociation(1L, 1L),
				new ScannedFileHandleAssociation(1L, 2L),
				new ScannedFileHandleAssociation(5L, 5L),
				new ScannedFileHandleAssociation(6L, 6L),
				new ScannedFileHandleAssociation(7L, 1L),
				new ScannedFileHandleAssociation(7L, 7L)
		);
		
		testScanRange(tableMapping, new IdRange(1, 10), batchSize,
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
	
	@Test
	public void testScanRangeWithSerializedEntity() throws IOException {

		String fileHandleIdColumn = "SERIALIZED_ENTITY";
		
		TableMapping<?> tableMapping = generateMapping(TABLE_NAME, DDL_BLOB_FILE_HANDLE, new FieldColumn[] {
				new FieldColumn("id", "ID", true).withIsBackupId(true),
				new FieldColumn("serializedEntity", fileHandleIdColumn).withHasFileHandleRef(true)
		});
		
		long batchSize = 2;
		
		// Emulate a compressed serialized entity that contains the file handle references

		RowMapperSupplier rowMapperSupplier = new SerializedFieldRowMapperSupplier<>(FileHandleHolder::deserialize, FileHandleHolder::getFileHandleIds);
		
		List<ScannedFileHandleAssociation> expected = Arrays.asList(
				new ScannedFileHandleAssociation(1L, 1L),
				new ScannedFileHandleAssociation(2L),
				new ScannedFileHandleAssociation(3L).withFileHandleIds(ImmutableSet.of(1L, 2L))
		);
		
		testScanRange(tableMapping, rowMapperSupplier, new IdRange(1, 10), batchSize,
			ImmutableList.of(
				// ID, SERIALIZED_ENTITY
				
				// First batch
				new Object[] { 1, FileHandleHolder.serialize(new FileHandleHolder(1L)) },
				// The serialized entity does not have any file handle, but it's still scanned (e.g. we do not know before deserializing that there are no file handles)
				new Object[] { 2, FileHandleHolder.serialize(new FileHandleHolder()) },
				
				// Second batch
				new Object[] { 3, FileHandleHolder.serialize(new FileHandleHolder(1L, 2L)) },
				// No serialized entity, this is skipped in the result
				new Object[] { 4, null }
			)
		, expected);
	}	
 	
	private void testGetIdRange(TableMapping<?> tableMapping, List<Object[]> data, IdRange expectedRange) throws IOException {
		ddlUtils.validateTableExists(tableMapping);
		
		FileHandleAssociationScanner scanner = getScannerInstance(tableMapping, DEFAULT_BATCH_SIZE, BasicFileHandleAssociationScanner::getDefaultRowMapper);
		
		// Call under test
		IdRange range =  scanner.getIdRange();
		
		assertEquals(new IdRange(-1, -1), range);
		
		// Generate some data
		addData(tableMapping, data);
		
		// Call under test
		range =  scanner.getIdRange();
		
		assertEquals(expectedRange, range);
	}
	
	private void testScanRange(TableMapping<?> tableMapping, IdRange range, long batchSize, List<Object[]> data, List<ScannedFileHandleAssociation> expected) throws IOException {
		testScanRange(tableMapping, BasicFileHandleAssociationScanner::getDefaultRowMapper, range, batchSize, data, expected);
	}
	
	private void testScanRange(TableMapping<?> tableMapping, RowMapperSupplier rowMapperSupplier, IdRange range, long batchSize, List<Object[]> data, List<ScannedFileHandleAssociation> expected) throws IOException {
		ddlUtils.validateTableExists(tableMapping);
		
		FileHandleAssociationScanner scanner = getScannerInstance(tableMapping, batchSize, rowMapperSupplier);
		
		// Call under test
		List<ScannedFileHandleAssociation> result = StreamSupport.stream(scanner.scanRange(range).spliterator(), false).collect(Collectors.toList());
		
		assertEquals(Collections.emptyList(), result);
		
		// Generate some data
		addData(tableMapping, data);
		
		// Call under test
		result = StreamSupport.stream(scanner.scanRange(range).spliterator(), false).collect(Collectors.toList());
		
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
	
	private FileHandleAssociationScanner getScannerInstance(TableMapping<?> tableMapping, long batchSize, RowMapperSupplier rowMapperSupplier) {
		return new BasicFileHandleAssociationScanner(jdbcTemplate, tableMapping, batchSize, rowMapperSupplier);
	}

	private static final class FileHandleHolder {
		
		private static final UnmodifiableXStream X_STREAM = UnmodifiableXStream.builder().allowTypes(FileHandleHolder.class).build();

		private List<Long> fileHandleIds;
		
		public FileHandleHolder() {
			
		}

		public FileHandleHolder(Long ...fileHandleIds) {
			if (fileHandleIds != null) {
				this.fileHandleIds = Arrays.asList(fileHandleIds);
			}
		}
		
		public Set<String> getFileHandleIds() {
			if (fileHandleIds == null || fileHandleIds.isEmpty()) {
				return Collections.emptySet();
			}
			return fileHandleIds.stream().map(String::valueOf).collect(Collectors.toSet());
		}
		
		public static FileHandleHolder deserialize(byte[] bytes) {
			try {
				return (FileHandleHolder) JDOSecondaryPropertyUtils.decompressObject(X_STREAM, bytes);
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}
		
		public static byte[] serialize(FileHandleHolder obj) {
			try {
				return JDOSecondaryPropertyUtils.compressObject(X_STREAM, obj);
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}

	}
}
