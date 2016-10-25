package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.util.ValidateArgument;

/**
 * An class representing requested facet columns that have been verified against its schema
 * @author zdong
 *
 */
public class ValidatedQueryFacetColumn {
	private String columnName;
	private FacetType facetType;
	private FacetColumnRequest facetColumnRequest;
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
	public ValidatedQueryFacetColumn(String columnName, FacetType facetType, FacetColumnRequest facetColumnRequest){
		ValidateArgument.required(columnName, "columnName");
		ValidateArgument.required(facetType, "facetType");
		
		//checks to make sure that useless parameters are not passed in
		if(facetColumnRequest != null){
			if(FacetType.enumeration.equals(facetType) && !(facetColumnRequest instanceof FacetColumnValuesRequest)){
				throw new IllegalArgumentException("facetColumnRequest was not an instance of FacetColumnValuesRequest");
			}
			if(FacetType.range.equals(facetType) && !(facetColumnRequest instanceof FacetColumnRangeRequest)){
				throw new IllegalArgumentException("facetColumnRequest was not an instance of FacetColumnRangeRequest");
			}
		}
		
		
		this.columnName = columnName;
		this.facetType = facetType;
		this.facetColumnRequest = facetColumnRequest;
		this.searchConditionString = createSearchConditionString();
	}

	public String getColumnName() {
		return this.columnName;
	}
	
	/**
	 * returns null if there were no filter requests associated with this column
	 * @return
	 */
	public FacetColumnRequest getFacetColumnRequest(){
		return this.facetColumnRequest;
	}
	
	/**
	 * returns null if no search conditions exist
	 * @return
	 */
	public String getSearchConditionString(){
		return this.searchConditionString;
	}
	
	public FacetType getFacetType(){
		return this.facetType;
	}
	
	private String createSearchConditionString(){
		switch (this.facetType){
		case enumeration:
			return createEnumerationSearchCondition(this.columnName, (FacetColumnValuesRequest) this.facetColumnRequest);
		case range:
			return createRangeSearchCondition(this.columnName, (FacetColumnRangeRequest) this.facetColumnRequest);
		default:
			throw new IllegalArgumentException("Unexpected FacetType");
		}
		
	}
	
	private static String createRangeSearchCondition(String columnName, FacetColumnRangeRequest facetRange){
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
	
	private static String createEnumerationSearchCondition(String columnName, FacetColumnValuesRequest facetValues){
		if(facetValues == null || facetValues.getFacetValues() == null|| facetValues.getFacetValues().isEmpty()){
			return null;
		}
		
		StringBuilder builder = new StringBuilder("(");
		int initialSize = builder.length();
		for(String value : facetValues.getFacetValues()){
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
