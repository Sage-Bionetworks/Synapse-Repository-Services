package org.sagebionetworks.web.shared;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Encapsulates search parameters
 * 
 * @author jmhill
 *
 */
public class SearchParameters implements IsSerializable{
	
	private List<String> selectColumns;
	private int offset;
	private int length;
	private String sort;
	private boolean ascending;
	
	public SearchParameters(){
	}
	public SearchParameters(List<String> selectColumns, int offset, int length,
			String sort, boolean ascending) {
		super();
		this.selectColumns = selectColumns;
		this.offset = offset;
		this.length = length;
		this.sort = sort;
		this.ascending = ascending;
	}
	public List<String> getSelectColumns() {
		return selectColumns;
	}
	public void setSelectColumns(List<String> selectColumns) {
		this.selectColumns = selectColumns;
	}
	public int getOffset() {
		return offset;
	}
	public void setOffset(int offset) {
		this.offset = offset;
	}
	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}
	public String getSort() {
		return sort;
	}
	public void setSort(String sort) {
		this.sort = sort;
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
		result = prime * result + length;
		result = prime * result + offset;
		result = prime * result
				+ ((selectColumns == null) ? 0 : selectColumns.hashCode());
		result = prime * result + ((sort == null) ? 0 : sort.hashCode());
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
		SearchParameters other = (SearchParameters) obj;
		if (ascending != other.ascending)
			return false;
		if (length != other.length)
			return false;
		if (offset != other.offset)
			return false;
		if (selectColumns == null) {
			if (other.selectColumns != null)
				return false;
		} else if (!selectColumns.equals(other.selectColumns))
			return false;
		if (sort == null) {
			if (other.sort != null)
				return false;
		} else if (!sort.equals(other.sort))
			return false;
		return true;
	}

}
