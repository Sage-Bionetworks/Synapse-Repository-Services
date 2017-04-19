package org.sagebionetworks.repo.model.query;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * The data describing a basic query.
 * 
 * @author jmhill
 *
 */
public class BasicQuery {
	
	List<String> select;
	String from;
	String sort;
	List<Expression> filters;
	boolean ascending = true;
	long offset = 0;
	long limit = 10;
	
	public BasicQuery(){
	}
	
	/**
	 * Copy constructor.
	 * 
	 * @param toCopy
	 */
	public BasicQuery(BasicQuery toCopy) {
		if(toCopy.select != null){
			this.select = new LinkedList<String>(toCopy.select);
		}
		this.from = toCopy.from;
		this.sort = toCopy.sort;
		if(toCopy.filters != null){
			this.filters = new LinkedList<Expression>(toCopy.filters);
		}
		this.ascending = toCopy.ascending;
		this.offset = toCopy.offset;
		this.limit = toCopy.limit;
	}
	
	public List<String> getSelect() {
		return select;
	}
	public void setSelect(List<String> select) {
		this.select = select;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
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
	public long getOffset() {
		return offset;
	}
	public void setOffset(long offset) {
		this.offset = offset;
	}
	public long getLimit() {
		return limit;
	}
	public void setLimit(long limit) {
		this.limit = limit;
	}
	public List<Expression> getFilters() {
		return filters;
	}
	public void setFilters(List<Expression> filters) {
		this.filters = filters;
	}
	
	public void addExpression(Expression filter){
		if(this.filters == null){
			this.filters = new ArrayList<Expression>();
		}
		this.filters.add(filter);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (ascending ? 1231 : 1237);
		result = prime * result + ((filters == null) ? 0 : filters.hashCode());
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + (int) (limit ^ (limit >>> 32));
		result = prime * result + (int) (offset ^ (offset >>> 32));
		result = prime * result + ((select == null) ? 0 : select.hashCode());
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
		BasicQuery other = (BasicQuery) obj;
		if (ascending != other.ascending)
			return false;
		if (filters == null) {
			if (other.filters != null)
				return false;
		} else if (!filters.equals(other.filters))
			return false;
		if (from == null) {
			if (other.from != null)
				return false;
		} else if (!from.equals(other.from))
			return false;
		if (limit != other.limit)
			return false;
		if (offset != other.offset)
			return false;
		if (select == null) {
			if (other.select != null)
				return false;
		} else if (!select.equals(other.select))
			return false;
		if (sort == null) {
			if (other.sort != null)
				return false;
		} else if (!sort.equals(other.sort))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "BasicQuery [select=" + select + ", from=" + from + ", sort="
				+ sort + ", filters=" + filters + ", ascending=" + ascending
				+ ", offset=" + offset + ", limit=" + limit + "]";
	}
	
}
