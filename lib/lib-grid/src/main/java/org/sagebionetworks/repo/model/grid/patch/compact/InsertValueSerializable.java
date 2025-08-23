package org.sagebionetworks.repo.model.grid.patch.compact;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertValue;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;

public class InsertValueSerializable implements OperationSerializable<InsertValue> {

	@Override
	public OperationType getType() {
		return OperationType.ins_val;
	}

	@Override
	public Class<? extends InsertValue> getTypeClass() {
		return InsertValue.class;
	}

	@Override
	public InsertValue deserialize(LogicalTimestamp id, JSONArray array) {
		Long replicaId = id.getReplicaId();
		LogicalTimestamp valueId = LogicalTimestampCompactSerializable.deserialize(replicaId, array, 1);
		LogicalTimestamp referenceId = LogicalTimestampCompactSerializable.deserialize(replicaId, array, 2);
		return new InsertValue(id, valueId, referenceId);
	}

	@Override
	public JSONArray serialize(InsertValue operation) {
		Long replicaId = operation.getOperationId().getReplicaId();
		JSONArray array = new JSONArray();
		array.put(OperationType.ins_val.getCode());
		array.put(LogicalTimestampCompactSerializable.serialize(replicaId, operation.getValueId()));
		array.put(LogicalTimestampCompactSerializable.serialize(replicaId, operation.getReferenceId()));
		return array;
	}

}
