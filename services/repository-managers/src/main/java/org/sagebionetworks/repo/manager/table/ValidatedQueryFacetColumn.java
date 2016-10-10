package org.sagebionetworks.repo.manager.table;

import java.util.HashSet;
import java.util.Set;

/**
 * An immutable class representing requested facet columns that have been verified against its schema
 * @author zdong
 *
 */
public class ValidatedQueryFacetColumn {
	private String columnId;
	private String columnName;
	private Set<String> columnValues;
	private boolean returnFacetCounts;
	private String valuesSearchConditionString;
	
	public ValidatedQueryFacetColumn(String columnId, String columnName, Set<String> columnValues, boolean returnFacetCounts){
		this.columnId = columnId;
		this.columnName = columnName;
		this.columnValues = new HashSet<>(columnValues);
		this.returnFacetCounts = returnFacetCounts;
		
		this.valuesSearchConditionString = createSearchConditionString(columnValues);
		
	}

	public Set<String> getColumnValues() {
		return new HashSet<>(columnValues);
	}

	public String getColumnName() {
		return columnName;
	}

	public String getColumnId() {
		return columnId;
	}
	
	public String getValuesSearchConditionString(){
		return valuesSearchConditionString;
	}
	
	public boolean returnFacetCounts(){
		return returnFacetCounts;
	}
	
	private String createSearchConditionString(Set<String> values){
		//TODO: change if using predicates instead of values
		StringBuilder builder = new StringBuilder("(");
		int initialSize = builder.length();
		for(String value : values){
			if(builder.length() > initialSize){
				builder.append(" OR ");
			}
			builder.append(columnName);
			builder.append("=");
			builder.append(value);
		}
		return builder.toString();
	}
	

}
