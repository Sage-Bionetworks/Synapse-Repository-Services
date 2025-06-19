package org.sagebionetworks.repo.manager.grid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.manager.grid.row.translator.ColumnTypeToConType;
import org.sagebionetworks.repo.manager.grid.row.translator.Translator;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.compact.PatchCompactSerializable;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertArray;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertObject;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertValue;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertVector;
import org.sagebionetworks.repo.model.grid.patch.operation.NewArray;
import org.sagebionetworks.repo.model.grid.patch.operation.NewConstant;
import org.sagebionetworks.repo.model.grid.patch.operation.NewObject;
import org.sagebionetworks.repo.model.grid.patch.operation.NewVector;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.util.ValidateArgument;

/**
 * A handler that can build a series of patches from a table row query.
 */
public class PatchRowHandler implements RowHandler {

	private final PatchStore patchStore;
	private final String sessionId;
	private final int rowsPerPatch;
	private final Translator[] translators;
	private final LogicalTimestamp rowId;
	private int rowCount;
	private Patch currentPatch;
	private LogicalTimestamp lastRowRef;

	public PatchRowHandler(PatchStore patchStore, String sessionId, Long replicaId, List<ColumnModel> schema,
			int rowsPerPatch) {
		super();
		ValidateArgument.required(patchStore, "patchStore");
		this.patchStore = patchStore;
		this.sessionId = sessionId;
		this.rowsPerPatch = rowsPerPatch;

		this.currentPatch = new Patch()
				.setPatchId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(1L));

		// initialize an empty document
		NewObject rootObject = currentPatch.addNewOperation(NewObject.class);
		NewConstant documnetVersion = currentPatch.addNewOperation(NewConstant.class)
				.setValue(new ConValue(ConType.STRING, "0.0.2"));
		NewVector columnNames = currentPatch.addNewOperation(NewVector.class);
		NewArray columnOrder = currentPatch.addNewOperation(NewArray.class);
		NewArray rows = currentPatch.addNewOperation(NewArray.class);
		rowId = rows.getOperationId();
		lastRowRef = rows.getOperationId();
		Map<String, LogicalTimestamp> objectMap = new LinkedHashMap<>();
		objectMap.put("doc_version", documnetVersion.getOperationId());
		objectMap.put("columnNames", columnNames.getOperationId());
		objectMap.put("columnOrder", columnOrder.getOperationId());
		objectMap.put("rows", rows.getOperationId());
		currentPatch.addNewOperation(InsertObject.class).setObjectId(rootObject.getOperationId()).setMap(objectMap);
		currentPatch.addNewOperation(InsertValue.class)
				.setReferenceId(new LogicalTimestamp().setReplicaId(0L).setSequenceNumber(0L))
				.setValueId(rootObject.getOperationId());

		if (!schema.isEmpty()) {
			translators = new Translator[schema.size()];
			// build the column names from the schema
			Map<Integer, LogicalTimestamp> columnNameMap = new LinkedHashMap<>();
			List<LogicalTimestamp> indexList = new ArrayList<>();
			for (int i = 0; i < schema.size(); i++) {
				ColumnModel cm = schema.get(i);
				// column name
				NewConstant nameConst = currentPatch.addNewOperation(NewConstant.class)
						.setValue(new ConValue(ConType.STRING, cm.getName()));
				columnNameMap.put(i, nameConst.getOperationId());
				// column index
				NewConstant indexConst = currentPatch.addNewOperation(NewConstant.class)
						.setValue(new ConValue(ConType.LONG, i));
				indexList.add(indexConst.getOperationId());

				translators[i] = ColumnTypeToConType.lookUpType(cm.getColumnType()).getTranslator();

			}
			currentPatch.addNewOperation(InsertVector.class).setVectorId(columnNames.getOperationId())
					.setMap(columnNameMap);
			currentPatch.addNewOperation(InsertArray.class).setArrayId(columnOrder.getOperationId())
					.setReferenceId(columnOrder.getOperationId()).setElementIds(indexList);
		} else {
			translators = new Translator[0];
		}
	}

	@Override
	public void nextRow(Row row) {
		this.rowCount++;
		NewVector rowVector = currentPatch.addNewOperation(NewVector.class);
		Map<Integer, LogicalTimestamp> cellValues = new LinkedHashMap<>();
		for (int i = 0; i < row.getValues().size(); i++) {
			String cellValue = row.getValues().get(i);
			NewConstant con = currentPatch.addNewOperation(NewConstant.class);
			con.setValue(translators[i].translateNullable(cellValue));
			cellValues.put(i, con.getOperationId());
		}
		currentPatch.addNewOperation(InsertVector.class).setVectorId(rowVector.getOperationId()).setMap(cellValues);
		InsertArray insertArray = currentPatch.addNewOperation(InsertArray.class).setArrayId(rowId)
				.setReferenceId(lastRowRef).setElementIds(Arrays.asList(rowVector.getOperationId()));
		lastRowRef = insertArray.getOperationId();

		if (this.rowCount >= rowsPerPatch) {
			saveCurrentPatch();
		}
	}

	void saveCurrentPatch() {
		if (currentPatch.getOperations() == null || currentPatch.getOperations().isEmpty()) {
			// do not save an empty patch
			return;
		}
		// save the current patch and create a new one.
		String patchBody = PatchCompactSerializable.serialize(currentPatch).toString();
		patchStore.savePatch(sessionId, currentPatch.getPatchId(), patchBody);

		// start a new patch with an ID = previous.clock+1;
		currentPatch = new Patch()
				.setPatchId(LogicalTimestamp.newIncrement(currentPatch.getPatchId(), currentPatch.getSpan()));
		this.rowCount = 0;
	}

	@Override
	public void close() throws IOException {
		saveCurrentPatch();
	}

}
