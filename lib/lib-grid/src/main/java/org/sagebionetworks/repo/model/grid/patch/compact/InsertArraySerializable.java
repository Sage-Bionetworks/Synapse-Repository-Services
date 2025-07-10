package org.sagebionetworks.repo.model.grid.patch.compact;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.InsertArray;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;

public class InsertArraySerializable implements OperationSerializable<InsertArray> {

	@Override
	public OperationType getType() {
		return OperationType.ins_arr;
	}

	@Override
	public Class<? extends InsertArray> getTypeClass() {
		return InsertArray.class;
	}

	@Override
	public InsertArray deserialize(LogicalTimestamp operationId, JSONArray array) {
		Long replicaId = operationId.getReplicaId();
		InsertArray insert = new InsertArray().setOperationId(operationId);
		insert.setArrayId(LogicalTimestampCompactSerializable.deserialize(replicaId, array, 1));
		insert.setReferenceId(LogicalTimestampCompactSerializable.deserialize(replicaId, array, 2));
		JSONArray elementsArray = array.getJSONArray(3);
		List<LogicalTimestamp> elements = new ArrayList<>(elementsArray.length());
		insert.setElementIds(elements);
		for (int i = 0; i < elementsArray.length(); i++) {
			elements.add(LogicalTimestampCompactSerializable.deserialize(replicaId, elementsArray, i));
		}
		return insert;
	}

	@Override
	public JSONArray serialize(InsertArray operation) {
		Long replicaId = operation.getOperationId().getReplicaId();
		JSONArray array = new JSONArray().put(OperationType.ins_arr.getCode());
		array.put(LogicalTimestampCompactSerializable.serialize(replicaId, operation.getArrayId()));
		array.put(LogicalTimestampCompactSerializable.serialize(replicaId, operation.getReferenceId()));
		JSONArray elements = new JSONArray();
		operation.getElementIds().forEach(e -> {
			elements.put(LogicalTimestampCompactSerializable.serialize(replicaId, e));
		});
		array.put(elements);
		return array;
	}

}
