package org.sagebionetworks.repo.manager.grid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
import org.sagebionetworks.repo.model.grid.patch.operation.immutable.ImmutableOperation;
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
		ImmutableOperation<NewObject> rootObject = currentPatch.addNewOperation(NewObject.class);
		NewConstant documentVersion = new NewConstant().setValue(new ConValue(ConType.STRING, "0.1.0"));
		currentPatch.addNewOperation(documentVersion);
		ImmutableOperation<NewVector> columnNames = currentPatch.addNewOperation(NewVector.class);
		ImmutableOperation<NewArray> columnOrder = currentPatch.addNewOperation(NewArray.class);
		ImmutableOperation<NewArray> rows = currentPatch.addNewOperation(NewArray.class);
		rowsArrayRef = rows.getOperationId();
		lastRowRef = rows.getOperationId();
		Map<String, LogicalTimestamp> objectMap = new LinkedHashMap<>();
		objectMap.put("doc_version", documentVersion.getOperationId());
		objectMap.put("columnNames", columnNames.getOperationId());
		objectMap.put("columnOrder", columnOrder.getOperationId());
		objectMap.put("rows", rows.getOperationId());
		InsertObject insertIntoRootObject = new InsertObject().setObjectId(rootObject.getOperationId()).setMap(objectMap);
		currentPatch.addNewOperation(insertIntoRootObject);
		InsertValue insertRootValue = new InsertValue()
				.setValueId(new LogicalTimestamp().setReplicaId(0L).setSequenceNumber(0L))
				.setReferenceId(rootObject.getOperationId());
		currentPatch.addNewOperation(insertRootValue);

		if (!schema.isEmpty()) {
			translators = new Translator[schema.size()];
			// build the column names from the schema
			Map<Integer, LogicalTimestamp> columnNameMap = new LinkedHashMap<>();
			List<LogicalTimestamp> indexList = new ArrayList<>();
			for (int i = 0; i < schema.size(); i++) {
				ColumnModel cm = schema.get(i);
				// column name
				NewConstant nameConst = new NewConstant()
						.setValue(new ConValue(ConType.STRING, cm.getName()));
				ImmutableOperation<NewConstant> insertedNameConst = currentPatch.addNewOperation(nameConst);
				columnNameMap.put(i, nameConst.getOperationId());
				// column index
				NewConstant columnIndex = new NewConstant()
						.setValue(new ConValue(ConType.LONG, i));
				ImmutableOperation<NewConstant> columnIndexOperation = currentPatch.addNewOperation(columnIndex);
				indexList.add(columnIndexOperation.getOperationId());

				translators[i] = ColumnTypeToConType.lookUpType(cm.getColumnType()).getTranslator();

			}
			currentPatch.addNewOperation(new InsertVector().setVectorId(columnNames.getOperationId())
					.setMap(columnNameMap));
			currentPatch.addNewOperation(new InsertArray().setArrayId(columnOrder.getOperationId())
					.setReferenceId(columnOrder.getOperationId()).setElementIds(indexList));
		} else {
			translators = new Translator[0];
		}
	}


	/**
	 * Adds the RowMetadata object to the patch. The row metadata has the following pseudo-schema. Fields that can be
	 * undefined are not guaranteed to be present.
	 * ```
	 * obj({
	 *     synapseRow: SynapseRowMetadataSchema | undefined
	 * })
	 * ```
	 *
	 * @param row the table query Row for which metadata should be extracted
	 * @return the NewObject containing the row metadata.
	 */
	private ImmutableOperation<NewObject> getRowMetadata(Row row) {
		ImmutableOperation<NewObject> metadataObjectForRow = currentPatch.addNewOperation(NewObject.class);

		// Create the "synapseRow" object
		ImmutableOperation<NewObject> synapseRowMetadata = getSynapseRowMetadata(row);

		// Attach the "synapseRow" object to the row metadata map
		Map<String, LogicalTimestamp> metadataMapForRow = new LinkedHashMap<>();
		metadataMapForRow.put("synapseRow", synapseRowMetadata.getOperationId());

		currentPatch.addNewOperation(new InsertObject().setObjectId(metadataObjectForRow.getOperationId())
				.setMap(metadataMapForRow)
		);

		return metadataObjectForRow;

	}

	/**
	 * Creates the SynapseRowMetadata object. The row metadata has the following pseudo-schema. Fields are not
	 * guaranteed to be present.
	 * ```
	 * s.obj({
	 *     rowId: s.const(double)
	 *     versionNumber: s.const(double)
	 *     etag: s.const(str)
	 * })
	 * ```
	 *
	 * @param row the table query Row for which Synapse Row metadata should be created
	 * @return the NewObject containing the synapseRowMetadata
	 */
	private ImmutableOperation<NewObject> getSynapseRowMetadata(Row row) {
		// Create the `synapseRow` object
		ImmutableOperation<NewObject> synapseRowMetadata = currentPatch.addNewOperation(NewObject.class);

		Map<String, LogicalTimestamp> synapseRowMetadataObjectMap = new LinkedHashMap<>();
		Long rowId = row.getRowId();
		if (rowId != null) {
			ImmutableOperation<NewConstant> rowIdConst = currentPatch.addNewOperation(new NewConstant().setValue(new ConValue(ConType.LONG, rowId)));
			synapseRowMetadataObjectMap.put("rowId", rowIdConst.getOperationId());
		}

		Long versionNumber = row.getVersionNumber();
		if (versionNumber != null) {
			ImmutableOperation<NewConstant> versionNumberConst = currentPatch.addNewOperation(new NewConstant().setValue(new ConValue(ConType.LONG, versionNumber)));
			synapseRowMetadataObjectMap.put("versionNumber", versionNumberConst.getOperationId());
		}

		String etag = row.getEtag();
		if (etag != null) {
			ImmutableOperation<NewConstant> etagConst = currentPatch.addNewOperation(new NewConstant().setValue(new ConValue(ConType.STRING, etag)));
			synapseRowMetadataObjectMap.put("etag", etagConst.getOperationId());
		}

		if (!synapseRowMetadataObjectMap.isEmpty()) {
			// fill the `synapseRow` object
			currentPatch.addNewOperation(new InsertObject()
                    .setObjectId(synapseRowMetadata.getOperationId())
					.setMap(synapseRowMetadataObjectMap));
		}
		return synapseRowMetadata;
	}

	/**
	 * Creates and returns a NewVector containing the values for the row.
	 *
	 * @param row the table query Row for which Synapse Row metadata should be created
	 * @return the NewVector containing the row values
	 */
	private ImmutableOperation<NewVector> getRowData(Row row) {
		ImmutableOperation<NewVector> rowValuesVector = currentPatch.addNewOperation(NewVector.class);
		Map<Integer, LogicalTimestamp> cellValues = new LinkedHashMap<>();
		for (int i = 0; i < row.getValues().size(); i++) {
			String cellValue = row.getValues().get(i);
			ImmutableOperation<NewConstant> con = currentPatch.addNewOperation(
					new NewConstant().setValue(translators[i].translateNullable(cellValue))
			);
			cellValues.put(i, con.getOperationId());
		}
		currentPatch.addNewOperation(new InsertVector()
				.setVectorId(rowValuesVector.getOperationId())
				.setMap(cellValues)
		);

		return rowValuesVector;
	}

	@Override
	public void nextRow(Row row) {
		this.rowCount++;
		ImmutableOperation<NewObject> rowObject = currentPatch.addNewOperation(NewObject.class);

		ImmutableOperation<NewVector> rowData = getRowData(row);
		ImmutableOperation<NewObject> rowMetadata = getRowMetadata(row);

		Map<String, LogicalTimestamp> rowObjectMap = new LinkedHashMap<>();
		rowObjectMap.put("data", rowData.getOperationId());
		rowObjectMap.put("metadata", rowMetadata.getOperationId());
		currentPatch.addNewOperation(new InsertObject().setObjectId(rowObject.getOperationId())
				.setMap(rowObjectMap));

		ImmutableOperation<InsertArray> insertArray = currentPatch.addNewOperation(new InsertArray().setArrayId(rowsArrayRef)
				.setReferenceId(lastRowRef)
				.setElementIds(Collections.singletonList(rowObject.getOperationId())));

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

	public int getRowsPerPatch() {
		return rowsPerPatch;
	}

}
