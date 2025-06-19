package org.sagebionetworks.repo.model.grid.patch.compact;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewString;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;

public class NewStringSerializable implements OperationSerializable<NewString> {

	@Override
	public OperationType getType() {
		return OperationType.new_str;
	}

	@Override
	public Class<? extends NewString> getTypeClass() {
		return NewString.class;
	}

	@Override
	public NewString deserialize(LogicalTimestamp id, JSONArray array) {
		return new NewString().setOperationId(id);
	}

	@Override
	public JSONArray serialize(NewString operation) {
		JSONArray array = new JSONArray();
		array.put(OperationType.new_str.getCode());
		return array;
	}

}
