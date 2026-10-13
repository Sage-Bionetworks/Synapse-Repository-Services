package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.manager.grid.PatchUtils;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.InsertRowChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangePublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.PatchBuilderPublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.UpdateMetadataChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.UpdateRowChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.merge.JoinedRow.GridRow;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.validation.GridRowValidator;
import org.sagebionetworks.repo.manager.grid.internal.replica.validation.JsonObjectSubject;
import org.sagebionetworks.repo.manager.schema.JsonSubject;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridCsvImportResponse;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Helper to process a stream of joined rows and publish the corresponding row changes
 */
@Service
public class JoinedRowChangePublisher {

	// The number of rows validated together. The schema validator is built once per batch.
	private static final int VALIDATION_BATCH_SIZE = 1000;

	private final PatchBuilderPublisher patchBuilderPublisher;
	private final GridIndexDao gridIndexDao;
	private final GridRowValidator gridRowValidator;
	private final int validationBatchSize;
	
	@Autowired
	public JoinedRowChangePublisher(PatchBuilderPublisher patchBuilderPublisher, GridIndexDao gridIndexDao, GridRowValidator gridRowValidator) {
		this(patchBuilderPublisher, gridIndexDao, gridRowValidator, VALIDATION_BATCH_SIZE);
	}
	
	JoinedRowChangePublisher(PatchBuilderPublisher patchBuilderPublisher, GridIndexDao gridIndexDao, GridRowValidator gridRowValidator, int validationBatchSize) {
		this.patchBuilderPublisher = patchBuilderPublisher;
		this.gridIndexDao = gridIndexDao;
		this.gridRowValidator = gridRowValidator;
		this.validationBatchSize = validationBatchSize;
	}
	
	/**
	 * Publish an insert for each joined row without a grid row and an update for each joined row with one.
	 * 
	 * @param validationSchema when present, each row is validated against this schema as it will be once the change
	 *                         is applied, and its validation state is published with the row's change
	 */
	public GridCsvImportResponse processJoinedRows(GridHeader header, GridConnectionInfo connInfo, Iterator<JoinedRow> joinedRowStream, ColumnMapping[] columnMapping, Optional<JsonSchema> validationSchema) {
		
		long rowCount = 0;
		long updatedCount = 0;
		long createdCount = 0;
		
		// Maps each column to the actual vector index defined in the grid
		Integer[] gridVectorIndex = Arrays.stream(columnMapping)
			.map(m -> header.getOrderedColumns().get(m.getGridIndex()).getVectorIndex())
			.toArray(Integer[]::new);
		
		List<String> columnNames = Arrays.stream(columnMapping).map(ColumnMapping::getColumnName).toList();
		
		try (IntendedChangePublisher changePublisher = new IntendedChangePublisher(connInfo, header.getClockSequenceMaximum(), patchBuilderPublisher, PatchUtils.MAX_CHANGE_SET_SIZE)) {
			LogicalTimestamp rowsArrayId = header.getRowsId();
			
			// The iterator of joined rows is sorted by upsert key descending, this allows us to use the current last row id
			// as the insert position for all new rows (since they will be inserted in reverse order)
			LogicalTimestamp lastRowId = gridIndexDao.getArrayLastNodeId(header.getSessionId(), header.getReplicaId(), rowsArrayId);
			
			List<JoinedRow> pendingRows = new ArrayList<>(validationBatchSize);
			
			while (joinedRowStream.hasNext()) {
				JoinedRow joinedRow = joinedRowStream.next();
				
				if (joinedRow.getGridRow() == null) {
					createdCount++;
				} else {
					updatedCount++;
				}
				
				pendingRows.add(joinedRow);
				
				if (pendingRows.size() >= validationBatchSize) {
					publishRows(changePublisher, pendingRows, rowsArrayId, lastRowId, gridVectorIndex, columnNames, validationSchema);
				}
				
				rowCount++;
			}
			
			publishRows(changePublisher, pendingRows, rowsArrayId, lastRowId, gridVectorIndex, columnNames, validationSchema);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
		
		return new GridCsvImportResponse()
			.setTotalCount(rowCount)
			.setUpdatedCount(updatedCount)
			.setCreatedCount(createdCount)
			.setSessionId(header.getSessionId());
	}
	
	void publishRows(IntendedChangePublisher changePublisher, List<JoinedRow> rows, LogicalTimestamp rowsArrayId, LogicalTimestamp lastRowId,
			Integer[] gridVectorIndex, List<String> columnNames, Optional<JsonSchema> validationSchema) {
		if (rows.isEmpty()) {
			return;
		}
		
		List<JSONObject> validationStates = validationSchema.map(schema -> validate(schema, rows, columnNames)).orElse(null);
		
		// Rows are published in the order they were read
		for (int i = 0; i < rows.size(); i++) {
			JSONObject validationState = validationStates == null ? null : validationStates.get(i);
			toChanges(rows.get(i), validationState, rowsArrayId, lastRowId, gridVectorIndex).forEach(changePublisher::publish);
		}
		
		rows.clear();
	}
	
	/**
	 * @param validationState the row's validation state, null when the row is not validated
	 * @return the changes that write the row, in the order they must be published: the row's validation state comes
	 *         after its cells, so the validation constant is newer than every cell the change writes
	 */
	static List<IntendedChange> toChanges(JoinedRow row, JSONObject validationState, LogicalTimestamp rowsArrayId, LogicalTimestamp lastRowId,
			Integer[] gridVectorIndex) {
		GridRow gridRow = row.getGridRow();
		
		if (gridRow == null) {
			return List.of(new InsertRowChange(rowsArrayId, lastRowId, row.getCsvData(), gridVectorIndex).setValidationState(validationState));
		}
		
		UpdateRowChange update = new UpdateRowChange(gridRow.vectorId(), row.getCsvData(), gridVectorIndex);
		
		// Without a row or metadata object there is nowhere to attach the state; the validation worker covers it
		if (validationState == null || (gridRow.objectId() == null && gridRow.metadataId() == null)) {
			return List.of(update);
		}
		
		return List.of(update, new UpdateMetadataChange()
			.setRowObjectId(gridRow.objectId())
			.setRowMetadataId(gridRow.metadataId())
			.setValidationState(validationState));
	}
	
	List<JSONObject> validate(JsonSchema schema, List<JoinedRow> rows, List<String> columnNames) {
		List<JsonSubject> subjects = rows.stream()
			.map(row -> (JsonSubject) new JsonObjectSubject(toRowJson(columnNames, row)))
			.toList();
		
		return gridRowValidator.validateBatch(schema, subjects).stream()
			.map(JDOSecondaryPropertyUtils::createJSONObjectForEntity)
			.toList();
	}
	
	// The JSON document the validation worker builds for the row once the change is applied: the grid row's cells
	// (none for a new row) with the CSV cells written over them. An undefined CSV cell clears the grid cell.
	static JSONObject toRowJson(List<String> columnNames, JoinedRow row) {
		JSONObject json = row.getGridRow() == null ? new JSONObject() : row.getGridRow().rowJsonObject();
		List<ConValue> csvData = row.getCsvData();
		for (int i = 0; i < columnNames.size(); i++) {
			ConValue value = csvData.get(i);
			if (value.isUndefined()) {
				json.remove(columnNames.get(i));
			} else {
				json.put(columnNames.get(i), value.getValue());
			}
		}
		return json;
	}
	
}
