package org.sagebionetworks.repo.web;
/**
 * Parameters for paginated results.
 * 
 * @author jmhill
 *
 */
public class PaginatedParameters {
	long offset = 0;
	long limit = 10;
	String sortBy = null;
	boolean ascending = true;;
	
	public PaginatedParameters(){
		
	}
	
	public PaginatedParameters(long offset, long limit, String sortBy,
			boolean ascending) {
		super();
		this.offset = offset;
		this.limit = limit;
		this.sortBy = sortBy;
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

	public String getSortBy() {
		return sortBy;
	}

	public void setSortBy(String sortBy) {
		this.sortBy = sortBy;
	}

	public boolean getAscending() {
		return ascending;
	}

	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}

}
