package org.sagebionetworks.repo.model.grid.patch.compact;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Timespan;
import org.sagebionetworks.repo.model.grid.patch.operation.Delete;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;

/**
 * See https://jsonjoy.com/specs/json-crdt-patch/encoding/compact-format#del-Operation-Encoding
 */
public class DeleteSerializable implements OperationSerializable<Delete> {

	@Override
	public OperationType getType() {
		return OperationType.del;
	}

	@Override
	public Class<? extends Delete> getTypeClass() {
		return Delete.class;
	}

	@Override
	public Delete deserialize(LogicalTimestamp operationId, JSONArray array) {
		Long replicaId = operationId.getReplicaId();
		
		// While this could be any RGA node (str, arr, bin), in practice we only support the arr case
		LogicalTimestamp nodeId = LogicalTimestampCompactSerializable.deserialize(replicaId, array, 1);
		// The last element is the array of "timespans"
		JSONArray timespanArray = array.getJSONArray(2);
		
		List<Timespan> timespans = new ArrayList<>(timespanArray.length());
		
		for (int i = 0; i < timespanArray.length(); i++) {
			JSONArray timespan = timespanArray.getJSONArray(i);
			LogicalTimestamp start;
			Long length;
			// This is the case where the replica id matches
			if (timespan.length() == 2) {
				start = new LogicalTimestamp()
					.setReplicaId(replicaId)
					.setSequenceNumber(timespan.getLong(0));
				length = timespan.getLong(1);
			} else {
				start = new LogicalTimestamp()
					.setReplicaId(timespan.getLong(0))
					.setSequenceNumber(timespan.getLong(1));
				length = timespan.getLong(2);
			}
			
			timespans.add(new Timespan(start, length));
		}
		
		return new Delete(operationId, nodeId, timespans);
	}

	@Override
	public JSONArray serialize(Delete operation) {
		Long replicaId = operation.getOperationId().getReplicaId();
		
		JSONArray array = new JSONArray()
			.put(OperationType.del.getCode())
			.put(LogicalTimestampCompactSerializable.serialize(replicaId, operation.getNodeId()));
		
		JSONArray timespanArray = new JSONArray();
		
		operation.getTimespans().forEach(ts -> {
			JSONArray timespan = new JSONArray();
			if (replicaId.equals(ts.getStart().getReplicaId())) {
				timespan.put(ts.getStart().getSequenceNumber());
			} else {
				timespan.put(ts.getStart().getReplicaId());
				timespan.put(ts.getStart().getSequenceNumber());
			}
			timespan.put(ts.getLength());
			timespanArray.put(timespan);
		});
		
		array.put(timespanArray);
		
		return array;
	}

}
