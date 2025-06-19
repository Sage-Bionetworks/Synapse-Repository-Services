package org.sagebionetworks.repo.model.grid.patch.compact;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.NewObject;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;

public class NewObjectSerializable implements OperationSerializable<NewObject> {

	@Override
	public OperationType getType() {
		return OperationType.new_obj;
	}

	@Override
	public Class<? extends NewObject> getTypeClass() {
		return NewObject.class;
	}

	@Override
	public NewObject deserialize(LogicalTimestamp id, JSONArray array) {
		return new NewObject().setOperationId(id);
	}

	@Override
	public JSONArray serialize(NewObject operation) {
		JSONArray array = new JSONArray();
		array.put(OperationType.new_obj.getCode());
		return array;
	}

}
