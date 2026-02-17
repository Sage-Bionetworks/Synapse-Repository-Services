package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import java.util.Map;
import java.util.Objects;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class UpdateColumnNames implements IntendedChange {
	
	private final LogicalTimestamp colunNamesVecId;
	private final Map<Integer, String> indexToNameMap;
	
	public UpdateColumnNames(LogicalTimestamp colunNamesVecId, Map<Integer, String> indexToNameMap) {
		super();
		this.colunNamesVecId = colunNamesVecId;
		this.indexToNameMap = indexToNameMap;
	}

	@Override
	public IntendedChangeType getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JSONObject toJson() {
		// TODO Auto-generated method stub
		return null;
	}

	public LogicalTimestamp getColunNamesVecId() {
		return colunNamesVecId;
	}

	public Map<Integer, String> getIndexToNameMap() {
		return indexToNameMap;
	}

	@Override
	public int hashCode() {
		return Objects.hash(colunNamesVecId, indexToNameMap);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UpdateColumnNames other = (UpdateColumnNames) obj;
		return Objects.equals(colunNamesVecId, other.colunNamesVecId)
				&& Objects.equals(indexToNameMap, other.indexToNameMap);
	}

	@Override
	public String toString() {
		return "UpdateColumnNames [colunNamesVecId=" + colunNamesVecId + ", indexToNameMap=" + indexToNameMap + "]";
	}

}
