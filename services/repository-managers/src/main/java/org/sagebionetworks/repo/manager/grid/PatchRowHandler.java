package org.sagebionetworks.repo.manager.grid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONArray;
import org.sagebionetworks.repo.manager.grid.row.translator.ColumnTypeToConType;
import org.sagebionetworks.repo.manager.grid.row.translator.Translator;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.compact.PatchCompactSerializable;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.Operations;
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
	private final LogicalTimestamp rowsArrayRef;
	private int rowCount;
	private Patch currentPatch;
	private LogicalTimestamp lastRowRef;

	public PatchRowHandler(PatchStore patchStore, String sessionId, Long replicaId, List<ColumnModel> schema,
			Long maxRowSizeBytes) {
		super();
		ValidateArgument.required(patchStore, "patchStore");
		this.patchStore = patchStore;
		this.sessionId = sessionId;
		this.rowsPerPatch = PatchUtils.calculateRowsPerPatch(maxRowSizeBytes);

		this.currentPatch = new Patch()
				.setPatchId(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(1L));

		// initialize an empty document

		LogicalTimestamp rootObjectOperationId = currentPatch.addNewOperation(Operations.newObject());
		LogicalTimestamp documentVersionOperationId = currentPatch.addNewOperation(Operations.newConstant().setValue(new ConValue(ConType.STRING, "0.1.0")));
		LogicalTimestamp columnNamesOperationId = currentPatch.addNewOperation(Operations.newVector());
		LogicalTimestamp columnOrderOperationId = currentPatch.addNewOperation(Operations.newArray());
		LogicalTimestamp rowsOperationId = currentPatch.addNewOperation(Operations.newArray());
		rowsArrayRef = rowsOperationId;
		lastRowRef = rowsOperationId;
		Map<String, LogicalTimestamp> objectMap = new LinkedHashMap<>();
		objectMap.put("doc_version", documentVersionOperationId);
		objectMap.put("columnNames", columnNamesOperationId);
		objectMap.put("columnOrder", columnOrderOperationId);
		objectMap.put("rows", rowsOperationId);
		currentPatch.addNewOperation(
				Operations.insertObject().setObjectId(rootObjectOperationId).setMap(objectMap)
		);
		currentPatch.addNewOperation(Operations.insertValue()
				.setValueId(new LogicalTimestamp().setReplicaId(0L).setSequenceNumber(0L))
				.setReferenceId(rootObjectOperationId)
		);

		if (!schema.isEmpty()) {
			translators = new Translator[schema.size()];
			// build the column names from the schema
			Map<Integer, LogicalTimestamp> columnNameMap = new LinkedHashMap<>();
			List<LogicalTimestamp> indexList = new ArrayList<>();
			for (int i = 0; i < schema.size(); i++) {
				ColumnModel cm = schema.get(i);
				// column name
				LogicalTimestamp nameConstRef = currentPatch.addNewOperation(Operations.newConstant().setValue(new ConValue(ConType.STRING, cm.getName())));
				columnNameMap.put(i, nameConstRef);
				// column index
                LogicalTimestamp columnIndexRef = currentPatch.addNewOperation(Operations.newConstant().setValue(new ConValue(ConType.LONG, i)));
				indexList.add(columnIndexRef);

				translators[i] = ColumnTypeToConType.lookUpType(cm.getColumnType()).getTranslator();

			}
			currentPatch.addNewOperation(Operations.insertVector()
					.setVectorId(columnNamesOperationId)
					.setMap(columnNameMap));
			currentPatch.addNewOperation(Operations.insertArray().setArrayId(columnOrderOperationId)
					.setReferenceId(columnOrderOperationId).setElementIds(indexList));
		} else {
			translators = new Translator[0];
		}
	}


	/**
	 * Adds the RowMetadata object to the patch. The row metadata has the following pseudo-schema. Fields that can be
	 * undefined are not guaranteed to be present.
	 * 
	 * ```
	 * obj({
	 *     rowValidation: s.const(json_object) | undefined
	 *     synapseRow: s.const(json_array) | undefined
	 * })
	 * ```
	 * The rowValidation metadata is not included during this boostrap phase.
	 * The synapseRow metadata is a constant with a serialized JSON array that contains 3 values in order:
	 * 
	 * [<rowId>, <versionNumber>, <etag>]
	 *
	 * @param row the table query Row for which metadata should be extracted
	 * @return a reference to the object node containing the row metadata if metadata is present, an empty Optional otherwise.
	 */
	private Optional<LogicalTimestamp> getRowMetadata(Row row) {
		
		// The synapse row information is the only metadata that might be included during this bootstrap phase.
		// The validation state is computed later on when the patches are applied
		if (row.getRowId() == null && row.getVersionNumber() == null && row.getEtag() == null) {
			return Optional.empty();
		}
		
		LogicalTimestamp metadataObjectForRowRef = currentPatch.addNewOperation(Operations.newObject());

		// Create the "synapseRow" JSON_ARRAY constant
		LogicalTimestamp synapseRowMetadataRef = currentPatch.addNewOperation(Operations.newConstant()
			.setValue(new ConValue(ConType.JSON_ARRAY, new JSONArray()
				    .put(row.getRowId())
					.put(row.getVersionNumber())
					.put(row.getEtag())
				)
			)
		);

		// Attach the "synapseRow" constant to the row metadata map
		Map<String, LogicalTimestamp> metadataMapForRow = new LinkedHashMap<>();
		metadataMapForRow.put("synapseRow", synapseRowMetadataRef);

		currentPatch.addNewOperation(Operations.insertObject()
				.setObjectId(metadataObjectForRowRef)
				.setMap(metadataMapForRow)
		);

		return Optional.of(metadataObjectForRowRef);

	}

	/**
	 * Creates and returns a NewVector containing the values for the row.
	 *
	 * @param row the table query Row for which Synapse Row metadata should be created
	 * @return a reference to the vector node containing the row values.
	 */
	private LogicalTimestamp getRowData(Row row) {
		LogicalTimestamp rowValuesVectorRef = currentPatch.addNewOperation(Operations.newVector());
		Map<Integer, LogicalTimestamp> cellValues = new LinkedHashMap<>();
		for (int i = 0; i < row.getValues().size(); i++) {
			String cellValue = row.getValues().get(i);
            LogicalTimestamp conRef = currentPatch.addNewOperation(
					Operations.newConstant().setValue(translators[i].translateNullable(cellValue))
			);
			cellValues.put(i, conRef);
		}
		currentPatch.addNewOperation(Operations.insertVector()
				.setVectorId(rowValuesVectorRef)
				.setMap(cellValues)
		);

		return rowValuesVectorRef;
	}

	@Override
	public void nextRow(Row row) {
		this.rowCount++;
		LogicalTimestamp rowObjectRef = currentPatch.addNewOperation(Operations.newObject());

        LogicalTimestamp rowDataRef = getRowData(row);

		Map<String, LogicalTimestamp> rowObjectMap = new LinkedHashMap<>();

		rowObjectMap.put("data", rowDataRef);
		
		getRowMetadata(row).ifPresent(rowMetadataRef -> {
			rowObjectMap.put("metadata", rowMetadataRef);
		});
		
		currentPatch.addNewOperation(Operations.insertObject()
			.setObjectId(rowObjectRef)
			.setMap(rowObjectMap)
		);

		LogicalTimestamp insertArrayRef = currentPatch.addNewOperation(Operations.insertArray()
			.setArrayId(rowsArrayRef)
			.setReferenceId(lastRowRef)
			.setElementIds(Collections.singletonList(rowObjectRef))
		);

		lastRowRef = insertArrayRef;

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

	public int getRowsPerPatch() {
		return rowsPerPatch;
	}

}
