package org.sagebionetworks.repo.manager.grid.internal.replica.export;

import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.manager.grid.internal.replica.validation.GridRowValidator;
import org.sagebionetworks.repo.manager.grid.internal.replica.validation.JsonObjectSubject;
import org.sagebionetworks.repo.manager.grid.internal.replica.validation.ValidationSummaryAccumulator;
import org.sagebionetworks.repo.manager.grid.util.GridJsonUtils;
import org.sagebionetworks.repo.manager.schema.JsonSubject;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.schema.ValidationSummaryStatistics;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Builds RecordSet artifacts (a CSV file and validation summary), ingesting each
 * row via {@link #addRow(Map)}. For each ingested row, this builder (1) writes
 * the row to the pushed RecordSet's data CSV and (2) validates the row inline
 * (in batches). Accumulated data is written to disk in temporary files.
 *
 * <p>
 * Not thread-safe; a single builder is fed by a single serialized traversal.
 */
public class RecordSetArtifactBuilder implements AutoCloseable {

	private final List<String> orderedColumnNames;
	private final JsonSchema validationSchema;
	private final GridRowValidator gridRowValidator;
	private final CSVWriter dataCsvWriter;
	private final CSVWriter validationCsvWriter;
	private final ValidationSummaryAccumulator validationSummary;
	private final File dataCsvFile;
	private final File validationDetailsFile;
	private final int batchSize;
	private final String recordSetId;

	private final List<Map<String, ConValue>> bufferedCells = new ArrayList<>();

	private boolean finished = false;
	private boolean closed = false;

	/**
	 * @param orderedColumnNames  the final (post-Phase-1) schema column names, in
	 *                            CSV output order
	 * @param validationSchema    the de-referenced validation schema, or null when
	 *                            the RecordSet has no bound schema (rows then count
	 *                            as "unknown")
	 * @param gridRowValidator    shared validator
	 * @param dataCsvWriter       writer for the pushed RecordSet data CSV (header
	 *                            written here)
	 * @param validationCsvWriter writer for the validation-details CSV (header
	 *                            written here)
	 * @param dataCsvFile         the temp file backing {@code dataCsvWriter}
	 * @param validationDetailsFile the temp file backing {@code validationCsvWriter}
	 * @param batchSize           number of rows to buffer before validating/flushing
	 * @param recordSetId         the RecordSet id, for the summary container id
	 */
	public RecordSetArtifactBuilder(List<String> orderedColumnNames, JsonSchema validationSchema, GridRowValidator gridRowValidator,
	                                CSVWriter dataCsvWriter, CSVWriter validationCsvWriter, File dataCsvFile, File validationDetailsFile,
	                                int batchSize, String recordSetId) throws IOException {
		this.orderedColumnNames = orderedColumnNames;
		this.validationSchema = validationSchema;
		this.gridRowValidator = gridRowValidator;
		this.dataCsvWriter = dataCsvWriter;
		this.dataCsvFile = dataCsvFile;
		this.validationDetailsFile = validationDetailsFile;
		this.batchSize = batchSize;
		this.recordSetId = recordSetId;
		this.dataCsvWriter.writeNext(orderedColumnNames.toArray(new String[0]));
		this.validationCsvWriter = validationCsvWriter;
		this.validationSummary = new ValidationSummaryAccumulator(validationCsvWriter);
	}

	/**
	 * Add a row's final cell values to the CSV that will be used for a new
	 * version of the RecordSet.
	 *
	 * @param cells the row's final cell values, keyed by column name
	 */
	public void addRow(Map<String, ConValue> cells) {
		if (finished) {
			throw new IllegalStateException("RecordSetArtifactBuilder has finished writing files, no more files can be added.");
		}
		bufferedCells.add(cells);
		if (bufferedCells.size() >= batchSize) {
			flush();
		}
	}

	/**
	 * For all buffered rows, compute validation results and write
	 */
	void flush() {
		if (bufferedCells.isEmpty()) {
			return;
		}
		List<ValidationResults> results = null;
		if (validationSchema != null) {
			List<JsonSubject> subjects = new ArrayList<>(bufferedCells.size());
			for (Map<String, ConValue> cells : bufferedCells) {
				subjects.add(new JsonObjectSubject(GridJsonUtils.gridRowToJsonObject(orderedColumnNames, cells)));
			}
			results = gridRowValidator.validateBatch(validationSchema, subjects);
		}

		try {
			for (int i = 0; i < bufferedCells.size(); i++) {
				Map<String, ConValue> cells = bufferedCells.get(i);
				ValidationResults result = results == null ? null : results.get(i);
				writeDataRow(cells);
				validationSummary.record(result);
			}
		} finally {
			bufferedCells.clear();
		}
	}

	private void writeDataRow(Map<String, ConValue> cells) {
		String[] row = new String[orderedColumnNames.size()];
		for (int i = 0; i < orderedColumnNames.size(); i++) {
			ConValue value = cells.get(orderedColumnNames.get(i));
			row[i] = (value == null || value.getValue() == null) ? null : value.getValue().toString();
		}
		try {
			dataCsvWriter.writeNext(row);
		} catch (IOException e) {
			throw new IllegalStateException("Could not write row to the push CSV file.", e);
		}
	}

	/**
	 * @return the accumulated validation summary. Only final after {@link #finish()}.
	 */
	public ValidationSummaryStatistics getValidationSummary() {
		return validationSummary.getValidationSummary(recordSetId);
	}

	/**
	 * @return the temp file holding the pushed RecordSet data CSV.
	 */
	public File getDataCsvFile() {
		return dataCsvFile;
	}

	/**
	 * @return the temp file holding the validation-details CSV.
	 */
	public File getValidationDetailsFile() {
		return validationDetailsFile;
	}

	/**
	 * Finalize the artifacts: flush the final partial batch and close both writers. After
	 * this returns, the temp files are complete and readable and the validation summary is
	 * final. Idempotent — a second call is a no-op. Does NOT delete the temp files (that is
	 * {@link #close()}'s job), so the files remain readable for upload.
	 */
	public void finish() throws IOException {
		if (finished) {
			return;
		}
		finished = true;
		// Flush the final partial batch before closing the writers.
		try {
			flush();
		} finally {
			// Nested try/finally so that a failure closing the first writer still closes the
			// second writer, while the first close's IOException still propagates.
			try {
				dataCsvWriter.close();
			} finally {
				validationCsvWriter.close();
			}
		}
	}

	/**
	 * Release everything this builder owns: {@link #finish() finishes} first if that has not
	 * already happened (so a failure-path close still releases the writer handles), then
	 * deletes both temp files. Idempotent — a second call is a no-op. After close, the
	 * builder's temp files are gone.
	 */
	@Override
	public void close() throws IOException {
		if (closed) {
			return;
		}
		closed = true;
		try {
			finish();
		} finally {
			if (dataCsvFile != null) {
				dataCsvFile.delete();
			}
			if (validationDetailsFile != null) {
				validationDetailsFile.delete();
			}
		}
	}
}
