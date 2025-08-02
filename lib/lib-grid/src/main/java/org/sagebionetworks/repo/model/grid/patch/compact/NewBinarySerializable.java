package org.sagebionetworks.repo.model.grid.patch.compact;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewBinary;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;

public class NewBinarySerializable implements OperationSerializable<NewBinary> {

	@Override
	public OperationType getType() {
		return OperationType.new_bin;
	}

	@Override
	public Class<? extends NewBinary> getTypeClass() {
		return NewBinary.class;
	}

	@Override
	public NewBinary deserialize(LogicalTimestamp id, JSONArray array) {
		return new NewBinary(id);
	}

	@Override
	public JSONArray serialize(NewBinary operation) {
		JSONArray array = new JSONArray();
		array.put(OperationType.new_bin.getCode());
		return array;
	}

}
