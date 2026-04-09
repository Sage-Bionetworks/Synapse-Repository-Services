package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.manager.grid.DocumentConstants;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.Operations;
import org.springframework.stereotype.Component;

@Component
public class InsertRowChangeHandler implements ChangeHandler<InsertRowChange> {

	public InsertRowChangeHandler() {
	}

	@Override
	public IntendedChangeType getType() {
		return IntendedChangeType.insert_row;
	}

	@Override
	public void handleChange(PatchBuilder builder, InsertRowChange change) {
		List<ConValue> rowData = change.getRowData();
		Integer[] rowVectorIndex = change.getRowVectorIndex();

		// The vector that contains the row data is wrapped into an object as the "data"
		// key value.
		// We need to create the object before the vector because the row object node id
		// needs to be
		// smaller than the vector id for the LWW insertion routine to succeed
		// (See
		// https://jsonjoy.com/specs/json-crdt/model-document/crdt-algorithms#Last-Write-Wins-(LWW)-CRDT-Algorithm)
		LogicalTimestamp rowObjectId = builder.addOperationBuilder(Operations.newObject());

		Map<String, LogicalTimestamp> rowObjectMap = new LinkedHashMap<>();

		// First creates the vector that represents the new row
		LogicalTimestamp rowVecId = builder.addOperationBuilder(Operations.newVector());
		rowObjectMap.put(DocumentConstants.DATA, rowVecId);
		Map<Integer, LogicalTimestamp> newVecConstantMap = new HashMap<>(rowData.size());

		for (int i = 0; i < rowData.size(); i++) {
			ConValue value = rowData.get(i);
			Integer vectorIndex = rowVectorIndex[i];
			LogicalTimestamp cellConstId = builder.addOperationBuilder(Operations.newConstant().setValue(value));
			newVecConstantMap.put(vectorIndex, cellConstId);
		}
		// Add the data to the vector
		builder.addOperationBuilder(Operations.insertVector().setVectorId(rowVecId).setMap(newVecConstantMap));

		if (change.getSynapseRow().isPresent()) {
			LogicalTimestamp metadataObjectForRowRef = handleSynapseRow(builder, change.getSynapseRow().get());
			rowObjectMap.put(DocumentConstants.METADATA, metadataObjectForRowRef);
		}

		builder.addOperationBuilder(Operations.insertObject().setObjectId(rowObjectId).setMap(rowObjectMap));

		// Finally add the row object to the array
		builder.addOperationBuilder(Operations.insertArray().setArrayId(change.getRowsArrayId())
				.setReferenceId(change.getNodeRefId()).setElementIds(List.of(rowObjectId)));
	}

	LogicalTimestamp handleSynapseRow(PatchBuilder builder, ConValue synapseRow) {
		LogicalTimestamp metadataObjectForRowRef = builder.addOperationBuilder(Operations.newObject());

		// Create the "synapseRow" JSON_ARRAY constant
		LogicalTimestamp synapseRowMetadataRef = builder
				.addOperationBuilder(Operations.newConstant().setValue(synapseRow));

		// Attach the "synapseRow" constant to the row metadata map
		Map<String, LogicalTimestamp> metadataMapForRow = new LinkedHashMap<>();
		metadataMapForRow.put(DocumentConstants.SYNAPSE_ROW, synapseRowMetadataRef);

		builder.addOperationBuilder(
				Operations.insertObject().setObjectId(metadataObjectForRowRef).setMap(metadataMapForRow));

		return metadataObjectForRowRef;
	}

}
