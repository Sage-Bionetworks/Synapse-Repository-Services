package org.sagebionetworks.repo.manager.grid.internal.replica.merge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.manager.grid.PatchUtils;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.InsertRowChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangePublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.PatchBuilderPublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.UpdateRowChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.validation.GridRowValidator;
import org.sagebionetworks.repo.manager.grid.internal.replica.validation.JsonObjectSubject;
import org.sagebionetworks.repo.manager.grid.util.GridJsonUtils;
import org.sagebionetworks.repo.manager.schema.JsonSubject;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridCsvImportResponse;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Helper to process a stream of joined rows and publish the corresponding row changes
 */
@Service
public class JoinedRowChangePublisher {

	// The number of inserted rows validated together. The schema validator is built once per batch.
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
	 * @param validationSchema when present, each inserted row carries its validation results against this
	 *                         schema, so the row and its validation state are written in the same patch
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
			
			List<InsertRowChange> pendingInserts = new ArrayList<>(validationBatchSize);
			
			while (joinedRowStream.hasNext()) {
				JoinedRow joinedRow = joinedRowStream.next();
				
				if (joinedRow.getGridRowVecId() == null) {
					pendingInserts.add(new InsertRowChange(rowsArrayId, lastRowId, joinedRow.getCsvData(), gridVectorIndex));
					if (pendingInserts.size() >= validationBatchSize) {
						publishInserts(changePublisher, pendingInserts, columnNames, validationSchema);
					}
					createdCount++;
				} else {
					changePublisher.publish(new UpdateRowChange(joinedRow.getGridRowVecId(), joinedRow.getCsvData(), gridVectorIndex));
					updatedCount++;
				}
				
				rowCount++;
			}
			
			publishInserts(changePublisher, pendingInserts, columnNames, validationSchema);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
		
		return new GridCsvImportResponse()
			.setTotalCount(rowCount)
			.setUpdatedCount(updatedCount)
			.setCreatedCount(createdCount)
			.setSessionId(header.getSessionId());
	}
	
	// Inserts are published in the order they were read; their relative order to updates does not matter since
	// each change targets a different row.
	void publishInserts(IntendedChangePublisher changePublisher, List<InsertRowChange> inserts, List<String> columnNames, Optional<JsonSchema> validationSchema) {
		if (inserts.isEmpty()) {
			return;
		}
		validationSchema.ifPresent(schema -> addValidationState(schema, inserts, columnNames));
		inserts.forEach(changePublisher::publish);
		inserts.clear();
	}
	
	// A new row only holds the CSV columns, so its JSON document matches the one the validation worker builds
	// from the grid.
	void addValidationState(JsonSchema schema, List<InsertRowChange> inserts, List<String> columnNames) {
		List<JsonSubject> subjects = inserts.stream()
			.map(insert -> (JsonSubject) new JsonObjectSubject(toRowJson(columnNames, insert.getRowData())))
			.toList();
		
		List<ValidationResults> results = gridRowValidator.validateBatch(schema, subjects);
		
		for (int i = 0; i < inserts.size(); i++) {
			inserts.get(i).setValidationState(JDOSecondaryPropertyUtils.createJSONObjectForEntity(results.get(i)));
		}
	}
	
	static JSONObject toRowJson(List<String> columnNames, List<ConValue> rowData) {
		Map<String, ConValue> cells = new HashMap<>(columnNames.size());
		for (int i = 0; i < columnNames.size(); i++) {
			cells.put(columnNames.get(i), rowData.get(i));
		}
		return GridJsonUtils.gridCellsToJsonObject(columnNames, cells);
	}
	
}
