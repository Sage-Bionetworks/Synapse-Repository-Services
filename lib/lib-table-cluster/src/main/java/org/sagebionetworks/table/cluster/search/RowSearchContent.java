package org.sagebionetworks.table.cluster.search;

import java.util.Objects;

/**
 * DTO for the search content of a table row
 */
public class RowSearchContent {
	
	private Long rowId;
	
	private String searchContent;

	public RowSearchContent(Long rowId, String searchContent) {
		this.rowId = rowId;
		this.searchContent = searchContent;
	}
	
	public Long getRowId() {
		return rowId;
	}
	
	public String getSearchContent() {
		return searchContent;
	}

	@Override
	public int hashCode() {
		return Objects.hash(rowId, searchContent);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		RowSearchContent other = (RowSearchContent) obj;
		return Objects.equals(rowId, other.rowId) && Objects.equals(searchContent, other.searchContent);
	}
	
}
