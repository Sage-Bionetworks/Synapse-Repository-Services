package org.sagebionetworks.repo.manager.table;

import java.util.HashSet;
import java.util.Set;

import org.sagebionetworks.repo.model.table.FacetRange;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.util.ValidateArgument;

/**
 * An immutable class representing requested facet columns that have been verified against its schema
 * @author zdong
 *
 */
public class ValidatedQueryFacetColumn {
	private String columnName;
	private FacetType facetType;
	private Set<String> columnValues;
	private FacetRange facetRange;
	private String searchConditionString;
	
	/**
	 * Constructor.
	 * Pass in 
	 * @param columnName name of the column
	 * @param facetType the type of the facet either enum or 
	 * @param columnValues the 
	 * @param facetRange 
	 * 
	 */
	public ValidatedQueryFacetColumn(String columnName, FacetType facetType, Set<String> columnValues, FacetRange facetRange){
		ValidateArgument.required(columnName, "columnName");
		ValidateArgument.required(facetType, "facetType");
		
		//checks to make sure that useless parameters are not passed in
		if(FacetType.enumeration.equals(facetType) && facetRange != null){
			throw new IllegalArgumentException("facetRange should be null if facetType is enumeration");
		}
		if(FacetType.range.equals(facetType) && columnValues != null){
			throw new IllegalArgumentException("columnValues should be null if facetType is range");
		}
		
		
		this.columnName = columnName;
		this.columnValues = (columnValues == null) ? null : new HashSet<>(columnValues);
		this.facetRange = (facetRange == null) ? null : copyFacetRange(facetRange);
		this.facetType = facetType;
		this.searchConditionString = createSearchConditionString();
	}
	
	private FacetRange copyFacetRange(FacetRange rangeToCopy){
		ValidateArgument.required(rangeToCopy, "rangeToCopy");
		
		FacetRange copiedRange = new FacetRange();
		copiedRange.setMax(rangeToCopy.getMax());
		copiedRange.setMin(rangeToCopy.getMin());
		return copiedRange;
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
	
	//returns null if no search conditions exist
	public String getSearchConditionString(){
		return this.searchConditionString;
	}
	
	public FacetType getFacetType(){
		return this.facetType;
	}
	
	public FacetRange getFacetRange(){
		return (this.facetRange == null) ? null : copyFacetRange(this.facetRange);
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
			builder.append("<=");
			appendValueToStringBuilder(builder, max);
		}else if (max == null){ //only min exists
			builder.append(">=");
			appendValueToStringBuilder(builder, min);
		}else{
			builder.append(" BETWEEN ");
			appendValueToStringBuilder(builder, min);
			builder.append(" AND ");
			appendValueToStringBuilder(builder, max);
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
			appendValueToStringBuilder(builder, value);
		}
		builder.append(")");
		return builder.toString();
	}
	
	/**
	 * Appends a value to the string builder
	 * and places single quotes (') around it if the string contains spaces
	 */ 
	private static void appendValueToStringBuilder(StringBuilder builder, String value){
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
