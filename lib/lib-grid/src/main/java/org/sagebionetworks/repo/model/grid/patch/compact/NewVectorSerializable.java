package org.sagebionetworks.repo.model.grid.patch.compact;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewVector;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;

public class NewVectorSerializable implements OperationSerializable<NewVector> {

	@Override
	public OperationType getType() {
		return OperationType.new_vec;
	}

	@Override
	public Class<? extends NewVector> getTypeClass() {
		return NewVector.class;
	}

	@Override
	public NewVector deserialize(LogicalTimestamp id, JSONArray array) {
		return new NewVector(id);
	}

	@Override
	public JSONArray serialize(NewVector operation) {
		JSONArray array = new JSONArray();
		array.put(OperationType.new_vec.getCode());
		return array;
	}

}
