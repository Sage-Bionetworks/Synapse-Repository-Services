package org.sagebionetworks.repo.manager.grid.internal.replica.export;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.internal.replica.validation.GridRowValidator;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.schema.ValidationSummaryStatistics;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

@ExtendWith(MockitoExtension.class)
public class RecordSetArtifactBuilderTest {

	@Mock
	private GridRowValidator mockValidator;
	@Mock
	private CSVWriter mockDataWriter;
	@Mock
	private CSVWriter mockValidationWriter;

	private static final List<String> COLUMNS = List.of("a", "b");

	private ConValue valueA(String v) {
		return new ConValue(ConType.STRING, v);
	}

	@Test
	public void testWritesHeadersOnConstruction() throws IOException {
		new RecordSetArtifactBuilder(COLUMNS, new JsonSchema(), mockValidator, mockDataWriter, mockValidationWriter, null, null, 1000,
				"syn1");
		verify(mockDataWriter).writeNext("a", "b");
		verify(mockValidationWriter)
				.writeNext("row_index", "is_valid", "validation_error_message", "all_validation_messages");
	}

	@Test
	public void testValidRowsAndSummary() throws IOException {
		JsonSchema schema = new JsonSchema();
		when(mockValidator.validateBatch(eq(schema), anyList())).thenReturn(List.of(
				new ValidationResults().setIsValid(true),
				new ValidationResults().setIsValid(false).setValidationErrorMessage("bad")
						.setAllValidationMessages(List.of("bad"))));

		RecordSetArtifactBuilder builder = new RecordSetArtifactBuilder(COLUMNS, schema, mockValidator, mockDataWriter, mockValidationWriter, null,
				null, 1000, "syn1");
		// call under test
		builder.addRow(Map.of("a", valueA("x"), "b", valueA("y")));
		builder.addRow(Map.of("a", valueA("z")));
		builder.close();

		// one batch validated
		verify(mockValidator, times(1)).validateBatch(eq(schema), anyList());

		ArgumentCaptor<String[]> dataCaptor = ArgumentCaptor.forClass(String[].class);
		verify(mockDataWriter, times(3)).writeNext(dataCaptor.capture());
		// header + 2 data rows, in finalSchema column order; missing cell is null
		assertArrayEquals(new String[] { "a", "b" }, dataCaptor.getAllValues().get(0));
		assertArrayEquals(new String[] { "x", "y" }, dataCaptor.getAllValues().get(1));
		assertArrayEquals(new String[] { "z", null }, dataCaptor.getAllValues().get(2));

		ValidationSummaryStatistics summary = builder.getValidationSummary();
		assertEquals("syn1", summary.getContainerId());
		assertEquals(2L, summary.getTotalNumberOfChildren());
		assertEquals(1L, summary.getNumberOfValidChildren());
		assertEquals(1L, summary.getNumberOfInvalidChildren());
		assertEquals(0L, summary.getNumberOfUnknownChildren());
	}

	@Test
	public void testNoSchemaCountsUnknown() throws IOException {
		RecordSetArtifactBuilder builder = new RecordSetArtifactBuilder(COLUMNS, null, mockValidator, mockDataWriter, mockValidationWriter, null,
				null, 1000, "syn1");
		// call under test
		builder.addRow(Map.of("a", valueA("x")));
		builder.close();

		// no schema -> no validation call
		verify(mockValidator, never()).validateBatch(any(), anyList());

		ValidationSummaryStatistics summary = builder.getValidationSummary();
		assertEquals(1L, summary.getTotalNumberOfChildren());
		assertEquals(0L, summary.getNumberOfValidChildren());
		assertEquals(0L, summary.getNumberOfInvalidChildren());
		assertEquals(1L, summary.getNumberOfUnknownChildren());
	}

	@Test
	public void testBatchingFlushesPerBatch() throws IOException {
		JsonSchema schema = new JsonSchema();
		when(mockValidator.validateBatch(eq(schema), anyList()))
				.thenReturn(List.of(new ValidationResults().setIsValid(true)));

		RecordSetArtifactBuilder builder = new RecordSetArtifactBuilder(COLUMNS, schema, mockValidator, mockDataWriter, mockValidationWriter, null,
				null, 1, "syn1");
		// call under test — batch size of 1 flushes each row immediately
		builder.addRow(Map.of("a", valueA("x")));
		builder.addRow(Map.of("a", valueA("z")));
		builder.close();

		// two full batches + an empty final flush (no-op)
		verify(mockValidator, times(2)).validateBatch(eq(schema), anyList());
		assertEquals(2L, builder.getValidationSummary().getTotalNumberOfChildren());
	}

	@Test
	public void testCloseDeletesBothTempFiles() throws IOException {
		File dataFile = File.createTempFile("push_data", ".csv");
		File validationFile = File.createTempFile("push_validation_details", ".csv");
		assertTrue(dataFile.exists());
		assertTrue(validationFile.exists());

		RecordSetArtifactBuilder builder = new RecordSetArtifactBuilder(COLUMNS, null, mockValidator, mockDataWriter,
				mockValidationWriter, dataFile, validationFile, 1000, "syn1");

		// call under test
		builder.close();

		verify(mockDataWriter).close();
		verify(mockValidationWriter).close();
		assertFalse(dataFile.exists());
		assertFalse(validationFile.exists());
	}

	@Test
	public void testCloseIsIdempotent() throws IOException {
		File dataFile = File.createTempFile("push_data", ".csv");
		File validationFile = File.createTempFile("push_validation_details", ".csv");

		RecordSetArtifactBuilder builder = new RecordSetArtifactBuilder(COLUMNS, null, mockValidator, mockDataWriter,
				mockValidationWriter, dataFile, validationFile, 1000, "syn1");

		builder.close();

		// call under test — a second close() is a no-op and does not throw
		assertDoesNotThrow(builder::close);

		// writers were only closed once
		verify(mockDataWriter, times(1)).close();
		verify(mockValidationWriter, times(1)).close();
		assertFalse(dataFile.exists());
		assertFalse(validationFile.exists());
	}

	@Test
	public void testFinishFlushesFinalBatchAndClosesWritersWithoutDeleting() throws IOException {
		File dataFile = File.createTempFile("push_data", ".csv");
		File validationFile = File.createTempFile("push_validation_details", ".csv");

		// batchSize larger than the row count so the row stays buffered until finish()
		RecordSetArtifactBuilder builder = new RecordSetArtifactBuilder(COLUMNS, null, mockValidator, mockDataWriter,
				mockValidationWriter, dataFile, validationFile, 1000, "syn1");
		builder.addRow(Map.of("a", valueA("x")));

		// call under test
		builder.finish();

		// finish() flushes the final partial batch (the buffered row is written)...
		verify(mockDataWriter).writeNext(new String[] { "x", null });
		// ...and closes both writers...
		verify(mockDataWriter).close();
		verify(mockValidationWriter).close();
		// ...but does NOT delete the temp files — they must remain readable for upload.
		assertTrue(dataFile.exists());
		assertTrue(validationFile.exists());

		dataFile.delete();
		validationFile.delete();
	}

	@Test
	public void testFinishIsIdempotent() throws IOException {
		RecordSetArtifactBuilder builder = new RecordSetArtifactBuilder(COLUMNS, null, mockValidator, mockDataWriter,
				mockValidationWriter, null, null, 1000, "syn1");
		builder.finish();

		// call under test — a second finish() is a no-op and does not re-close the writers
		assertDoesNotThrow(builder::finish);

		verify(mockDataWriter, times(1)).close();
		verify(mockValidationWriter, times(1)).close();
	}

	@Test
	public void testCloseAfterFinishDeletesFilesWithoutReclosingWriters() throws IOException {
		File dataFile = File.createTempFile("push_data", ".csv");
		File validationFile = File.createTempFile("push_validation_details", ".csv");

		RecordSetArtifactBuilder builder = new RecordSetArtifactBuilder(COLUMNS, null, mockValidator, mockDataWriter,
				mockValidationWriter, dataFile, validationFile, 1000, "syn1");
		builder.finish();

		// call under test — close() after finish() deletes the files but does not re-close writers
		builder.close();

		verify(mockDataWriter, times(1)).close();
		verify(mockValidationWriter, times(1)).close();
		assertFalse(dataFile.exists());
		assertFalse(validationFile.exists());
	}

	@Test
	public void testCloseWithoutFinishFinishesThenDeletes() throws IOException {
		File dataFile = File.createTempFile("push_data", ".csv");
		File validationFile = File.createTempFile("push_validation_details", ".csv");

		RecordSetArtifactBuilder builder = new RecordSetArtifactBuilder(COLUMNS, null, mockValidator, mockDataWriter,
				mockValidationWriter, dataFile, validationFile, 1000, "syn1");
		builder.addRow(Map.of("a", valueA("x")));

		// call under test — close() on a never-finished builder finishes first (flush + close
		// writers) so no writer handle leaks, then deletes.
		builder.close();

		verify(mockDataWriter).writeNext(new String[] { "x", null });
		verify(mockDataWriter).close();
		verify(mockValidationWriter).close();
		assertFalse(dataFile.exists());
		assertFalse(validationFile.exists());
	}

	@Test
	public void testFinishThenReadThenCloseEndToEnd() throws IOException {
		// Real writers over real files so we can read the finalized CSV back. batchSize=2
		// with 3 rows means the final partial batch (1 row) is only written by finish().
		File dataFile = File.createTempFile("push_data", ".csv");
		File validationFile = File.createTempFile("push_validation_details", ".csv");
		CSVWriter dataWriter = new CSVWriter(new java.io.FileWriter(dataFile));
		CSVWriter validationWriter = new CSVWriter(new java.io.FileWriter(validationFile));

		RecordSetArtifactBuilder builder = new RecordSetArtifactBuilder(COLUMNS, null, mockValidator, dataWriter,
				validationWriter, dataFile, validationFile, 2, "syn1");
		builder.addRow(Map.of("a", valueA("r1a"), "b", valueA("r1b")));
		builder.addRow(Map.of("a", valueA("r2a"), "b", valueA("r2b")));
		builder.addRow(Map.of("a", valueA("r3a"), "b", valueA("r3b")));

		// call under test — finish() must flush the trailing partial batch and close writers
		builder.finish();

		// Read the finalized data CSV: header + 3 rows.
		List<String[]> rows;
		try (CSVReader reader = new CSVReader(new FileReader(dataFile))) {
			rows = reader.readAll();
		}
		assertEquals(4, rows.size());
		assertArrayEquals(new String[] { "a", "b" }, rows.get(0));
		assertArrayEquals(new String[] { "r1a", "r1b" }, rows.get(1));
		assertArrayEquals(new String[] { "r2a", "r2b" }, rows.get(2));
		assertArrayEquals(new String[] { "r3a", "r3b" }, rows.get(3));

		// The summary counts all 3 rows (no schema -> all unknown).
		ValidationSummaryStatistics summary = builder.getValidationSummary();
		assertEquals(3L, summary.getTotalNumberOfChildren());
		assertEquals(3L, summary.getNumberOfUnknownChildren());

		// close() reaps the files.
		builder.close();
		assertFalse(dataFile.exists());
		assertFalse(validationFile.exists());
	}

	@Test
	public void testAddRowAfterCloseThrowsException() throws IOException {
		RecordSetArtifactBuilder builder = new RecordSetArtifactBuilder(COLUMNS, null, mockValidator, mockDataWriter, mockValidationWriter, null,
				null, 1, "syn1");
		builder.close();

		// call under test
		assertThrows(IllegalStateException.class, () -> builder.addRow(Map.of("a", valueA("x"))));
	}
}
