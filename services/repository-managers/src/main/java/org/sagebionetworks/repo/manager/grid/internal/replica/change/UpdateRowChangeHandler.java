package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.Operations;
import org.springframework.stereotype.Component;

@Component
public class UpdateRowChangeHandler implements ChangeHandler<UpdateRowChange> {

	public UpdateRowChangeHandler() {}

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
	}

}
