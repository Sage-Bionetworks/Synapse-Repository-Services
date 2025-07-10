package org.sagebionetworks.repo.model.grid.patch.compact;

import org.json.JSONArray;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.operation.Operation;
import org.sagebionetworks.repo.model.grid.patch.operation.OperationType;

public interface OperationSerializable<T extends Operation<?>> {
	
	OperationType getType();
	
	Class<? extends T> getTypeClass();
	
	T deserialize(LogicalTimestamp operationId, JSONArray array);
	
	JSONArray serialize(T operation);

}
