package org.sagebionetworks.table.cluster;

public final class SQLScopeFilter {
	
	private final String viewTypeFilter;
	
	private final String viewScopeFilterColumn;

	protected SQLScopeFilter(String viewTypeFilter, String viewScopeFilterColumn) {
		this.viewTypeFilter = viewTypeFilter;
		this.viewScopeFilterColumn = viewScopeFilterColumn;
	}
	
	public String getViewTypeFilter() {
		return viewTypeFilter;
	}
	
	public String getViewScopeFilterColumn() {
		return viewScopeFilterColumn;
	}

	

}
