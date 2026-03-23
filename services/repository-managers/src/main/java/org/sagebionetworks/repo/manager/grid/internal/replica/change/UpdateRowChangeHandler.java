package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.manager.grid.DocumentConstants;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.Operations;
import org.springframework.stereotype.Component;

@Component
public class UpdateRowChangeHandler implements ChangeHandler<UpdateRowChange> {

	public UpdateRowChangeHandler() {
	}

	@Override
	public IntendedChangeType getType() {
		return IntendedChangeType.update_row;
	}

	@Override
	public void handleChange(PatchBuilder builder, UpdateRowChange change) {
		LogicalTimestamp rowVecId = change.getRowVectorId();
		List<ConValue> rowData = change.getRowData();
		Integer[] rowVecIndex = change.getRowVectorIndex();

		Map<Integer, LogicalTimestamp> updatedConstantMap = new HashMap<>();

		for (int i = 0; i < rowData.size(); i++) {
			ConValue value = rowData.get(i);
			Integer vectorIndex = rowVecIndex[i];

			LogicalTimestamp cellConstId = builder.addOperationBuilder(Operations.newConstant().setValue(value));

			updatedConstantMap.put(vectorIndex, cellConstId);
		}

		builder.addOperationBuilder(Operations.insertVector().setVectorId(rowVecId).setMap(updatedConstantMap));

		if (change.getSynapseRow().isPresent() && change.getMetadataObjectId().isPresent()) {
			LogicalTimestamp conId = builder
					.addOperationBuilder(Operations.newConstant().setValue(change.getSynapseRow().get()));
			builder.addOperationBuilder(Operations.insertObject().setObjectId(change.getMetadataObjectId().get())
					.setMap(Map.of(DocumentConstants.SYNAPSE_ROW, conId)));
		}
	}

}
