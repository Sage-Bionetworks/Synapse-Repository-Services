package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.LinkedHashMap;
import java.util.Map;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.Operations;
import org.springframework.stereotype.Component;

@Component
public class UpdateColumnNamesChangeHandler implements ChangeHandler<UpdateColumnNamesChange> {

	@Override
	public IntendedChangeType getType() {
		return IntendedChangeType.update_column_names;
	}

	@Override
	public void handleChange(PatchBuilder builder, UpdateColumnNamesChange change) {
		// Create a map of vector index -> constant reference for each column name
		Map<Integer, LogicalTimestamp> columnNameMap = new LinkedHashMap<>();

		change.getIndexToNameMap().forEach((index, conValue) -> {
			LogicalTimestamp nameConstRef = builder.addOperationBuilder(Operations.newConstant().setValue(conValue));
			columnNameMap.put(index, nameConstRef);
		});

		// Insert all column names into the vector at once
		builder.addOperationBuilder(
				Operations.insertVector().setVectorId(change.getColunNamesVecId()).setMap(columnNameMap));
	}

}
