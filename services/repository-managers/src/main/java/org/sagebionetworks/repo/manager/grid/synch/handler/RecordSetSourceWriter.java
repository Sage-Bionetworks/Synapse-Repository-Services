package org.sagebionetworks.repo.manager.grid.synch.handler;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.grid.internal.replica.export.RecordSetArtifactBuilder;
import org.sagebionetworks.repo.manager.grid.internal.replica.export.GridRecordSetExporter;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.validation.GridRowValidator;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItem;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.asynch.AsyncJobProgressCallback;
import org.sagebionetworks.repo.model.grid.SyncType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;

/**
 * {@link SourceWriter} strategy for a RecordSet source. The RecordSet is never
 * mutated in place. The final rows are accumulated ({@link #beginPush} →
 * {@link #recordFinalRowState} → {@link #completePush}) into a new exported
 * RecordSet CSV version.
 */
public class RecordSetSourceWriter implements SourceWriter {

	private static final Logger LOG = LogManager.getLogger(RecordSetSourceWriter.class);

	private final UserInfo user;
	private final GridRecordSetExporter recordSetExporter;
	private final GridRowValidator gridRowValidator;
	private final RecordSet recordSet;
	private final CsvTableDescriptor csvDescriptor;
	private final String schemaId;
	private final String recordSetId;
	private final boolean pushEnabled;

	// Non-null only for a PULL_PUSH run, between beginPush and close. Accumulates the
	// surviving grid rows into the pushed RecordSet version's artifacts.
	private RecordSetArtifactBuilder pushBuilder;

	public RecordSetSourceWriter(UserInfo user, GridRecordSetExporter recordSetExporter,
	                             GridRowValidator gridRowValidator, RecordSet recordSet,
	                             CsvTableDescriptor csvDescriptor, String schemaId, SyncType syncType) {
		this.user = user;
		this.recordSetExporter = recordSetExporter;
		this.gridRowValidator = gridRowValidator;
		this.recordSet = recordSet;
		this.csvDescriptor = csvDescriptor;
		this.schemaId = schemaId;
		this.recordSetId = recordSet.getId();
		this.pushEnabled = SyncType.PULL_PUSH.equals(syncType);
	}

	// The RecordSet is mutated only by the push (export) step, so the merge's
	// source-mutating operations are no-ops. All data is accumulated via recordFinalRowState

	@Override
	public void addNewRowToSource(RowSourceItem row) {
		// no-op
	}

	@Override
	public void removeRow(RowSourceItem row) {
		// no-op
	}

	@Override
	public void addColumnToSource(String columnName) {
		// no-op
	}

	@Override
	public void removeColumn(String columnName) {
		// no-op
	}

	@Override
	public void applyCellChangesFromCopyToSource(String rowId, Map<String, ConValue> changedCells) {
		// no-op
	}

	/**
	 * For a PULL_PUSH run, creates the artifact builder keyed to the final
	 * schema so the finalized rows fed via {@link #recordFinalRowState} are validated
	 * and written to a new CSV. For PULL, this is a no-op (nothing is pushed).
	 */
	@Override
	public void beginPush(AsyncJobProgressCallback callback, List<Column> finalSchema) throws IOException {
		if (!pushEnabled) {
			return;
		}
		List<String> columnNames = finalSchema.stream().map(Column::getName).collect(Collectors.toList());
		JsonSchema validationSchema = schemaId == null ? null : gridRowValidator.getValidationSchema(schemaId);
		this.pushBuilder = recordSetExporter.createArtifactBuilder(callback.getJobId(), columnNames, validationSchema,
				csvDescriptor, recordSetId);
	}

	@Override
	public void recordFinalRowState(Map<String, ConValue> finalCells) {
		if (pushBuilder != null) {
			pushBuilder.addRow(finalCells);
		}
	}

	/**
	 * Flushes the push artifact as a new RecordSet CSV version (PULL_PUSH only) and
	 * returns the new version number. Empty for PULL.
	 */
	@Override
	public Optional<Long> completePush() throws Exception {
		if (pushBuilder == null) {
			return Optional.empty();
		}
		// Finalize the artifacts and pass to `pushFromArtifactBuilder` which will update the RecordSet
		pushBuilder.finish();
		RecordSet updated = recordSetExporter.pushFromArtifactBuilder(user, recordSet, pushBuilder);
		return Optional.of(updated.getVersionNumber());
	}

	@Override
	public List<String> getErrorMessages() {
		return Collections.emptyList();
	}

	@Override
	public void close() {
		if (pushBuilder != null) {
			try {
				pushBuilder.close();
			} catch (Exception e) {
				LOG.warn("Failed to close push artifact builder during cleanup", e);
			}
			pushBuilder = null;
		}
	}

}
