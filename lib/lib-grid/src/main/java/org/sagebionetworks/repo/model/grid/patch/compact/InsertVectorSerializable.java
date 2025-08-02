package org.sagebionetworks.repo.model.grid.patch.compact;

import java.util.LinkedHashMap;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertVector;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;

public class InsertVectorSerializable implements OperationSerializable<InsertVector> {

	@Override
	public OperationType getType() {
		return OperationType.ins_vec;
	}

	@Override
	public Class<? extends InsertVector> getTypeClass() {
		return InsertVector.class;
	}

	@Override
	public InsertVector deserialize(LogicalTimestamp operationId, JSONArray array) {
		Long replicaId = operationId.getReplicaId();
		LogicalTimestamp vectorId = LogicalTimestampCompactSerializable.deserialize(replicaId, array, 1);
		JSONArray mapArray = array.getJSONArray(2);
		LinkedHashMap<Integer, LogicalTimestamp> map = new LinkedHashMap<>(mapArray.length());
		for (int i = 0; i < mapArray.length(); i++) {
			JSONArray value = mapArray.getJSONArray(i);
			Integer key = value.getInt(0);
			LogicalTimestamp valueId = LogicalTimestampCompactSerializable.deserialize(replicaId, value, 1);
			map.put(key, valueId);
		}
		return new InsertVector(operationId, vectorId, map);
	}

	@Override
	public JSONArray serialize(InsertVector operation) {
		Long replicaId = operation.getOperationId().getReplicaId();
		JSONArray array = new JSONArray();
		array.put(OperationType.ins_vec.getCode());
		array.put(LogicalTimestampCompactSerializable.serialize(replicaId, operation.getVectorId()));
		JSONArray mapArray = new JSONArray();
		operation.getMap().forEach((k, v) -> {
			mapArray.put(new JSONArray().put(k).put(LogicalTimestampCompactSerializable.serialize(replicaId, v)));
		});
		array.put(mapArray);
		return array;
	}

}
