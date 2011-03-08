package org.sagebionetworks.web.shared;

import java.util.List;

import org.sagebionetworks.web.shared.QueryConstants.ObjectType;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Encapsulates search parameters
 * 
 * @author jmhill
 *
 */
public class SearchParameters implements IsSerializable{
	
	private List<String> selectColumns;
	private ObjectType fromType;
	private int offset = 1;
	private int limit = 10;
	private String sort;
	private boolean ascending;
	/**
	 * Currently the query service only supports one of these.
	 * In the future we support multiples with 'and' and 'or'
	 */
	private WhereCondition where;
	
	public SearchParameters(){
	}
	public SearchParameters(List<String> selectColumns, String fromType, WhereCondition where, int offset, int limit,
			String sort, boolean ascending) {
		super();
		this.fromType = ObjectType.valueOf(fromType);
		this.selectColumns = selectColumns;
		this.where = where;
		this.offset = offset;
		this.limit = limit;
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
	public int getLimit() {
		return limit;
	}
	public void setLimit(int limit) {
		this.limit = limit;
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
	/**
	 * Strings are allowed for RPC serialization, but enums are not.
	 * @return
	 */
	public String getFromType() {
		return fromType.name();
	}
	/**
	 * Using "fetch" instead of "get" to work around the
	 * problem with enums and RPC serialization.
	 * @return
	 */
	public ObjectType fetchType(){
		return fromType;
	}
	public void setFromType(String fromType) {
		this.fromType = ObjectType.valueOf(fromType);
	}
	
	public WhereCondition getWhere() {
		return where;
	}
	public void setWhere(WhereCondition where) {
		this.where = where;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (ascending ? 1231 : 1237);
		result = prime * result
				+ ((fromType == null) ? 0 : fromType.hashCode());
		result = prime * result + limit;
		result = prime * result + offset;
		result = prime * result
				+ ((selectColumns == null) ? 0 : selectColumns.hashCode());
		result = prime * result + ((sort == null) ? 0 : sort.hashCode());
		result = prime * result + ((where == null) ? 0 : where.hashCode());
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
		if (fromType != other.fromType)
			return false;
		if (limit != other.limit)
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
		if (where == null) {
			if (other.where != null)
				return false;
		} else if (!where.equals(other.where))
			return false;
		return true;
	}


}
