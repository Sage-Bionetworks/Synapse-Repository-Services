package org.sagebionetworks.repo.manager.grid.internal.replica.model;

import java.util.Objects;

import org.json.JSONArray;

public class RowData {

	private JSONArray data;

	public JSONArray getData() {
		return data;
	}

	public RowData setData(JSONArray data) {
		this.data = data;
		return this;
	}

	String dataToJson() {
		return data != null ? data.toString() : null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(dataToJson());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RowData other = (RowData) obj;
		return Objects.equals(dataToJson(), other.dataToJson());
	}

	@Override
	public String toString() {
		return "RowData [data=" + data + "]";
	}

}
