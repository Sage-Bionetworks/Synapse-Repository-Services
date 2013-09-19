package org.sagebionetworks.repo.model.query;

import java.util.ArrayList;
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
	
}
