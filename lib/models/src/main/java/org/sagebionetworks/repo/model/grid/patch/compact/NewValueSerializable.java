package org.sagebionetworks.repo.model.grid.patch.compact;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewValue;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;

public class NewValueSerializable implements OperationSerializable<NewValue> {

	@Override
	public OperationType getType() {
		return OperationType.new_val;
	}

	@Override
	public Class<? extends NewValue> getTypeClass() {
		return NewValue.class;
	}

	@Override
	public NewValue deserialize(LogicalTimestamp id, JSONArray array) {
		return new NewValue().setOperationId(id);
	}

	@Override
	public JSONArray serialize(NewValue operation) {
		JSONArray array = new JSONArray();
		array.put(OperationType.new_val.getCode());
		return array;
	}

}
