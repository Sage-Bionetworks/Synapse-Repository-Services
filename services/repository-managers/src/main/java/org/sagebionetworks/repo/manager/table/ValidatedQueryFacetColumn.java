package org.sagebionetworks.repo.manager.table;

import java.util.HashSet;
import java.util.Set;

/**
 * A class representing requested facet columns that have been verified against its schema
 * @author zdong
 *
 */
public class ValidatedQueryFacetColumn {
	private String columnId;
	private String columnName;
	private Set<String> columnValues;
	private boolean returnFacetCounts;
	
	public ValidatedQueryFacetColumn(String columnId, String columnName, Set<String> columnValues, boolean returnFacetCounts){
		this.columnId = columnId;
		this.columnName = columnName;
		this.columnValues = new HashSet<>(columnValues);
		this.returnFacetCounts = returnFacetCounts;
	}

	public Set<String> getColumnValues() {
		return columnValues;
	}

	public String getColumName() {
		return columnName;
	}

	public String getColumnId() {
		return columnId;
	}
	
	public boolean returnFacetCounts(){
		return returnFacetCounts;
	}
	

}
