package org.sagebionetworks.repo.manager.table;

import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.table.FacetRange;
import org.sagebionetworks.repo.model.table.FacetType;

/**
 * An immutable class representing requested facet columns that have been verified against its schema
 * @author zdong
 *
 */
public class ValidatedQueryFacetColumn {
	private String columnName;
	private Set<String> columnValues;
	private FacetRange facetRange;
	private FacetType facetType;
	private String valuesSearchConditionString;
	
	/**
	 * Constructor.
	 * @param columnName
	 * @param columnValues
	 * @param facetRange 
	 * @param facetType
	 */
	public ValidatedQueryFacetColumn(String columnName, FacetType facetType, Set<String> columnValues, FacetRange facetRange){
		this.columnName = columnName;
		this.columnValues = (columnValues == null) ? null : new HashSet<>(columnValues);
		this.facetRange = facetRange;
		this.facetType = facetType;
		this.valuesSearchConditionString = createSearchConditionString();
	}
	
	/**
	 * Returns a copy of the columnValues of this object or null if it does not have one
	 * @return
	 */
	public Set<String> getColumnValues() {
		return (this.columnValues == null) ? null : new HashSet<>(this.columnValues);
	}

	public String getColumnName() {
		return this.columnName;
	}
	
	//returns null if no search conditions are applied
	public String getSearchConditionString(){
		return this.valuesSearchConditionString;
	}
	
	public FacetType getFacetType(){
		return this.facetType;
	}
	
	public FacetRange getFacetRange(){
		return this.facetRange;
	}
	
	private String createSearchConditionString(){
		switch (this.facetType){
		case enumeration:
			return createEnumerationSearchCondition(this.columnName, this.columnValues);
		case range:
			return createRangeSearchCondition(this.columnName, this.facetRange);
		default:
			throw new IllegalArgumentException("Unexpected FacetType");
		}
		
	}
	
	private static String createRangeSearchCondition(String columnName, FacetRange facetRange){
		if(facetRange == null || ( (facetRange.getMin() == null || facetRange.getMin().equals(""))
									&& (facetRange.getMax() == null || facetRange.getMax().equals("")) ) ){
			return null;
		}
		String min = facetRange.getMin();
		String max = facetRange.getMax();
		
		StringBuilder builder = new StringBuilder("(");
		
		//at this point we know at least one value is not null and is not empty string
		builder.append(columnName);
		if(min == null){ //only max exists
			builder.append(" <= ");
			appendValueToStringBuilder(max, builder);
		}else if (max == null){ //only min exists
			builder.append(" >= ");
			appendValueToStringBuilder(min, builder);
		}else{
			builder.append(" BETWEEN ");
			appendValueToStringBuilder(min, builder);
			builder.append(" AND ");
			appendValueToStringBuilder(max, builder);
		}
		
		builder.append(")");
		return builder.toString();
	}
	
	private static String createEnumerationSearchCondition(String columnName, Set<String> values){
		if(values == null || values.isEmpty()){
			return null;
		}
		
		StringBuilder builder = new StringBuilder("(");
		int initialSize = builder.length();
		for(String value : values){
			if(builder.length() > initialSize){
				builder.append(" OR ");
			}
			builder.append(columnName);
			builder.append("=");
			appendValueToStringBuilder(value, builder);
		}
		return builder.toString();
	}
	
	private static void appendValueToStringBuilder(String value, StringBuilder builder){
		boolean containsSpaces = value.contains(" ");
		if(containsSpaces){
			builder.append("'");
		}
		builder.append(value);
		if(containsSpaces){
			builder.append("'");
		}
	}
	

}
