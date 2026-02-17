package org.sagebionetworks.repo.manager.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.schema.JsonSchemaValidationManager;
import org.sagebionetworks.repo.manager.schema.JsonSubject;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.util.FileProvider;

@ExtendWith(MockitoExtension.class)
public class SnapshotRowHandlerTest {

	@Mock
	private SnapshotStore mockSnapshotStore;

	@Mock
	private FileProvider mockFileProvider;

	@Mock
	private JsonSchemaValidationManager mockValidationManager;

	@TempDir
	File tempDir;

	private String sessionId;
	private Long replicaId;
	private List<ColumnModel> schema;
	private List<Integer> requiredColumnIndices;
	private Long createdByUserId;
	private File tempFile;

	@BeforeEach
	public void before() throws IOException {
		sessionId = "s123";
		replicaId = 19L;
		schema = List.of(new ColumnModel().setColumnType(ColumnType.STRING).setName("aString"),
				new ColumnModel().setColumnType(ColumnType.INTEGER).setName("anInt"));
		requiredColumnIndices = Collections.emptyList();
		createdByUserId = 999L;

		// Create a real temp file for testing
		tempFile = new File(tempDir, "snapshot-test.cbor");

		lenient().when(mockFileProvider.createTempFile("snapshot", ".cbor")).thenReturn(tempFile);
	}

	@Test
	public void testNoColumnsNoRows() throws IOException {
		schema = Collections.emptyList();

		// call under test
		try (SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, createdByUserId, mockValidationManager, null)) {
			// no row to add
		}

		verifyFileCreatedUploadedAndDeleted();
		verifySnapshotSaved();
	}

	@Test
	public void testWithColumnNoRows() throws IOException {
		// call under test
		try (SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, createdByUserId, mockValidationManager, null)) {
			// no row to add
		}

		verifyFileCreatedUploadedAndDeleted();
		verifySnapshotSaved();
	}

	@Test
	public void testWithRows() throws IOException {
		// call under test
		try (SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, createdByUserId, mockValidationManager, null)) {
			handler.nextRow(
					new Row().setValues(Arrays.asList("one", "101")).setRowId(1L).setVersionNumber(4L).setEtag("fake-etag-1"));
			handler.nextRow(
					new Row().setValues(Arrays.asList("two", "202")).setRowId(2L).setVersionNumber(5L).setEtag("fake-etag-2"));
			handler.nextRow(new Row().setValues(Arrays.asList("three", "303")).setRowId(3L).setVersionNumber(6L)
					.setEtag("fake-etag-3"));
		}

		verifyFileCreatedUploadedAndDeleted();
		verifySnapshotSaved();
	}

	@Test
	public void testWithRowsPartialMetadata() throws IOException {

		// call under test
		try (SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, createdByUserId, mockValidationManager, null)) {
			// Row with partial metadata
			handler.nextRow(new Row().setValues(Arrays.asList("four", "404")).setRowId(3L).setEtag("fake-etag-4"));
			// Row with no metadata
			handler.nextRow(new Row().setValues(Arrays.asList("five", "505")));
		}

		verifyFileCreatedUploadedAndDeleted();
		verifySnapshotSaved();
	}

	@Test
	public void testEachType() throws IOException {

		boolean hasDefault = false;
		schema = TableModelTestUtils.createOneOfEachType(hasDefault);
		List<Row> rows = TableModelTestUtils.createRows(schema, 3,
				new TableModelTestUtils.ValueOptions().includeSpace(false));

		// call under test
		try (SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, createdByUserId, mockValidationManager, null)) {
			rows.forEach(r -> {
				handler.nextRow(r);
			});
		}

		verifyFileCreatedUploadedAndDeleted();
		verifySnapshotSaved();
	}

	@Test
	public void testWriteNullOrUndefinedUsingRequiredColumnIndices() throws Exception {

		requiredColumnIndices = List.of(1); // only the second column is required

		// call under test
		try (SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, createdByUserId, mockValidationManager, null)) {
			handler.nextRow(
					new Row().setValues(Arrays.asList(null, null)).setRowId(1L).setVersionNumber(4L).setEtag("fake-etag-1"));
		}

		verifyFileCreatedUploadedAndDeleted();
		verifySnapshotSaved();
	}

	@Test
	public void testConstructorWithNullSnapshotStore() {
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			new SnapshotRowHandler(null, sessionId, replicaId, schema, requiredColumnIndices, mockFileProvider,
					createdByUserId, mockValidationManager, null);
		});
	}

	@Test
	public void testConstructorFailsToCreateTempFile() throws IOException {
		when(mockFileProvider.createTempFile("snapshot", ".cbor")).thenThrow(new IOException("Failed to create temp file"));

		// call under test
		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema, requiredColumnIndices,
					mockFileProvider, createdByUserId, mockValidationManager, null);
		});

		assertEquals("Failed to create temporary file for snapshot", ex.getMessage());
	}

	@Test
	public void testWithValidationSchema() throws IOException {
		// Setup validation schema and manager
		JsonSchema validationSchema = new JsonSchema();
		ValidationResults validResults = new ValidationResults();
		validResults.setIsValid(true);
		validResults.setObjectId("test-id");
		validResults.setObjectType(null);
		validResults.setObjectEtag("test-etag");

		when(mockValidationManager.validate(eq(validationSchema), any(JsonSubject.class)))
				.thenReturn(validResults);

		// call under test
		try (SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, createdByUserId, mockValidationManager, validationSchema)) {
			handler.nextRow(new Row().setValues(Arrays.asList("one", "101"))
					.setRowId(1L).setVersionNumber(4L).setEtag("fake-etag-1"));
		}

		verifyFileCreatedUploadedAndDeleted();
		verifySnapshotSaved();

		// Verify validation was called once
		verify(mockValidationManager, times(1)).validate(eq(validationSchema), any(JsonSubject.class));
	}

	@Test
	public void testWithNullValidationSchema() throws IOException {
		// call under test - null validation schema should skip validation
		try (SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, createdByUserId, mockValidationManager, null)) {
			handler.nextRow(new Row().setValues(Arrays.asList("one", "101"))
					.setRowId(1L).setVersionNumber(4L).setEtag("fake-etag-1"));
		}

		verifyFileCreatedUploadedAndDeleted();
		verifySnapshotSaved();

		// Verify validation was NOT called
		verify(mockValidationManager, never()).validate(any(), any());
	}

	@Test
	public void testValidationWithMultipleRows() throws IOException {
		// Setup validation
		JsonSchema validationSchema = new JsonSchema();
		ValidationResults validResults = new ValidationResults();
		validResults.setIsValid(true);

		when(mockValidationManager.validate(eq(validationSchema), any(JsonSubject.class)))
				.thenReturn(validResults);

		// call under test - multiple rows should each get validated
		try (SnapshotRowHandler handler = new SnapshotRowHandler(mockSnapshotStore, sessionId, replicaId, schema,
				requiredColumnIndices, mockFileProvider, createdByUserId, mockValidationManager, validationSchema)) {
			handler.nextRow(new Row().setValues(Arrays.asList("one", "101")));
			handler.nextRow(new Row().setValues(Arrays.asList("two", "202")));
			handler.nextRow(new Row().setValues(Arrays.asList("three", "303")));
		}

		verifyFileCreatedUploadedAndDeleted();
		verifySnapshotSaved();

		// Verify validation was called 3 times (once per row)
		verify(mockValidationManager, times(3)).validate(eq(validationSchema), any(JsonSubject.class));
	}

	private void verifyFileCreatedUploadedAndDeleted() throws IOException {
		verify(mockFileProvider).createTempFile("snapshot", ".cbor");
		verify(mockSnapshotStore).saveSnapshot(eq(sessionId), any(ClockTable.class), eq(createdByUserId), eq(tempFile));
		assertTrue(!tempFile.exists(), "Temp file should be deleted after close");
	}

	private void verifySnapshotSaved() {
		ArgumentCaptor<ClockTable> clockTableCaptor = ArgumentCaptor.forClass(ClockTable.class);
		verify(mockSnapshotStore).saveSnapshot(eq(sessionId), clockTableCaptor.capture(), eq(createdByUserId), eq(tempFile));

		ClockTable clockTable = clockTableCaptor.getValue();
		assertNotNull(clockTable, "Clock table should not be null");
	}

}
