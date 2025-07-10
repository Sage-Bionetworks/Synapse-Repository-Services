package org.sagebionetworks.repo.model.grid.patch.compact;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewArray;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;

public class NewArraySerializable implements OperationSerializable<NewArray> {

	@Override
	public OperationType getType() {
		return OperationType.new_arr;
	}

	@Override
	public Class<? extends NewArray> getTypeClass() {
		return NewArray.class;
	}

	@Override
	public NewArray deserialize(LogicalTimestamp id, JSONArray array) {
		return new NewArray().setOperationId(id);
	}

	@Override
	public JSONArray serialize(NewArray operation) {
		JSONArray array = new JSONArray();
		array.put(OperationType.new_arr.getCode());
		return array;
	}

}
