package org.sagebionetworks.repo.web;
/**
 * Parameters for paginated results.
 * 
 * @author jmhill
 *
 */
public class PaginatedParameters {
	int offset = 0;
	int limit = 10;
	String sortBy = null;
	boolean ascending = true;;
	
	public PaginatedParameters(){
		
	}
	
	public PaginatedParameters(int offset, int limit, String sortBy,
			boolean ascending) {
		super();
		this.offset = offset;
		this.limit = limit;
		this.sortBy = sortBy;
		this.ascending = ascending;
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
