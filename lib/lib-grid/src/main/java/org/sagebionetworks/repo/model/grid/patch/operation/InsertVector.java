package org.sagebionetworks.repo.model.grid.patch.operation;

import java.util.Map;
import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class InsertVector implements Operation<InsertVector> {

	private LogicalTimestamp operationId;
	private LogicalTimestamp vectorId;
	private Map<Integer, LogicalTimestamp> map;

	@Override
	public OperationType getType() {
		return OperationType.ins_vec;
	}

	@Override
	public LogicalTimestamp getOperationId() {
		return operationId;
	}

	@Override
	public long getSpan() {
		return 1;
	}

	public LogicalTimestamp getVectorId() {
		return vectorId;
	}

	public InsertVector setVectorId(LogicalTimestamp vectorId) {
		this.vectorId = vectorId;
		return this;
	}

	public Map<Integer, LogicalTimestamp> getMap() {
		return map;
	}

	public InsertVector setMap(Map<Integer, LogicalTimestamp> map) {
		this.map = map;
		return this;
	}

	public InsertVector setOperationId(LogicalTimestamp operationId) {
		this.operationId = operationId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(map, operationId, vectorId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InsertVector other = (InsertVector) obj;
		return Objects.equals(map, other.map) && Objects.equals(operationId, other.operationId)
				&& Objects.equals(vectorId, other.vectorId);
	}

	@Override
	public String toString() {
		return "InsertVector [operationId=" + operationId + ", vectorId=" + vectorId + ", map=" + map + "]";
	}

}
