package org.sagebionetworks.web.client.view;

import java.util.List;
import java.util.Map;

/**
 * Encapsulates the row data sent to a dynamic table view.
 * 
 * @author jmhill
 *
 */
public class RowData {
	
	private List<Map<String, Object>> rows;
	private int offset;
	private int limit;
	private int totalCount;
	private String sortKey;
	private boolean ascending;
	
	public RowData(){}
	
	public RowData(List<Map<String, Object>> rows, int offset, int limit,
			int totalCount, String sortKey, boolean ascending) {
		super();
		this.rows = rows;
		this.offset = offset;
		this.limit = limit;
		this.totalCount = totalCount;
		this.sortKey = sortKey;
		this.ascending = ascending;
	}
	public List<Map<String, Object>> getRows() {
		return rows;
	}
	public void setRows(List<Map<String, Object>> rows) {
		this.rows = rows;
	}
	public int getOffset() {
		return offset;
	}
	public void setOffset(int offset) {
		this.offset = offset;
	}
	public int getLimit() {
		return limit;
	}
	public void setLimit(int limit) {
		this.limit = limit;
	}
	public int getTotalCount() {
		return totalCount;
	}
	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}
	public String getSortKey() {
		return sortKey;
	}
	public void setSortKey(String sortKey) {
		this.sortKey = sortKey;
	}
	public boolean isAscending() {
		return ascending;
	}
	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (ascending ? 1231 : 1237);
		result = prime * result + limit;
		result = prime * result + offset;
		result = prime * result + ((rows == null) ? 0 : rows.hashCode());
		result = prime * result + ((sortKey == null) ? 0 : sortKey.hashCode());
		result = prime * result + totalCount;
		return result;
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
		if (ascending != other.ascending)
			return false;
		if (limit != other.limit)
			return false;
		if (offset != other.offset)
			return false;
		if (rows == null) {
			if (other.rows != null)
				return false;
		} else if (!rows.equals(other.rows))
			return false;
		if (sortKey == null) {
			if (other.sortKey != null)
				return false;
		} else if (!sortKey.equals(other.sortKey))
			return false;
		if (totalCount != other.totalCount)
			return false;
		return true;
	}

}
