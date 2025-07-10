package org.sagebionetworks.repo.model.grid.patch.compact;

import java.util.LinkedHashMap;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertObject;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;

public class InsertObjectSerializable implements OperationSerializable<InsertObject> {

	@Override
	public OperationType getType() {
		return OperationType.ins_obj;
	}

	@Override
	public Class<? extends InsertObject> getTypeClass() {
		return InsertObject.class;
	}

	@Override
	public InsertObject deserialize(LogicalTimestamp operationId, JSONArray array) {
		Long replicaId = operationId.getReplicaId();
		InsertObject operation = new InsertObject().setOperationId(operationId)
				.setObjectId(LogicalTimestampCompactSerializable.deserialize(replicaId, array, 1));
		JSONArray mapArray = array.getJSONArray(2);
		LinkedHashMap<String, LogicalTimestamp> map = new LinkedHashMap<>(mapArray.length());
		for (int i = 0; i < mapArray.length(); i++) {
			JSONArray value = mapArray.getJSONArray(i);
			String key = value.getString(0);
			LogicalTimestamp valueId = LogicalTimestampCompactSerializable.deserialize(replicaId, value, 1);
			map.put(key, valueId);
		}
		operation.setMap(map);
		return operation;
	}

	@Override
	public JSONArray serialize(InsertObject operation) {
		Long replicaId = operation.getOperationId().getReplicaId();
		JSONArray array = new JSONArray();
		array.put(OperationType.ins_obj.getCode());
		array.put(LogicalTimestampCompactSerializable.serialize(replicaId, operation.getObjectId()));
		JSONArray mapArray = new JSONArray();
		operation.getMap().forEach((k, v) -> {
			mapArray.put(new JSONArray().put(k).put(LogicalTimestampCompactSerializable.serialize(replicaId, v)));
		});
		array.put(mapArray);
		return array;
	}

}
