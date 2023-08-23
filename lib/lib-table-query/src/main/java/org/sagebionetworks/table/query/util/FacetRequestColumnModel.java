package org.sagebionetworks.table.query.util;

import static org.sagebionetworks.repo.model.table.TableConstants.NULL_VALUE_KEYWORD;

import java.util.StringJoiner;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.JsonSubColumnModel;
import org.sagebionetworks.util.ValidateArgument;

/**
 * An class representing requested facet columns that have been verified against its schema
 * @author zdong
 *
 */
public class FacetRequestColumnModel {
	private String columnName;
	private FacetType facetType;
	private FacetColumnRequest facetColumnRequest;
	private String searchConditionString;
	private boolean columnTypeIsList;
	private String jsonPath;
	
	/**
	 * Constructor.
	 * @param columnModel The original ColumnModel from which we derive the FacetRequestColumnModel
	 * @param facetColumnRequest The FacetColumnRequest describing the requested facet.
	 * 
	 */
	public FacetRequestColumnModel(ColumnModel columnModel, FacetColumnRequest facetColumnRequest){
		ValidateArgument.required(columnModel, "columnModel");
		ValidateArgument.required(columnModel.getName(), "columnModel.name");
		ValidateArgument.required(columnModel.getColumnType(), "columnModel.columnType");
		ValidateArgument.required(columnModel.getFacetType(), "columnModel.facetType");
		ValidateArgument.requirement(facetColumnRequest == null  || columnModel.getName().equals(facetColumnRequest.getColumnName()), "names of the columns must match");
		
		//checks to make sure that useless parameters are not passed in
		validateFacetRequestType(columnModel.getFacetType(), facetColumnRequest);

		this.facetType = columnModel.getFacetType();
		this.columnName = columnModel.getName();
		this.facetColumnRequest = facetColumnRequest;
		this.columnTypeIsList = ColumnTypeListMappings.isList(columnModel.getColumnType());
		this.searchConditionString = createFacetSearchConditionString(facetColumnRequest, this.columnTypeIsList, null);
	}
	
	public FacetRequestColumnModel(String columnName, JsonSubColumnModel subColumn, FacetColumnRequest facetColumnRequest) {
		ValidateArgument.required(columnName, "The columnName");
		ValidateArgument.required(subColumn.getFacetType(), "subColumn.facetType");
		ValidateArgument.requiredNotBlank(subColumn.getJsonPath(), "subColumn.jsonPath");
		ValidateArgument.required(subColumn.getColumnType(), "subColumn.columnType");
		
		validateFacetRequestType(subColumn.getFacetType(), facetColumnRequest);
		
		if (facetColumnRequest != null) {
			ValidateArgument.requirement(subColumn.getJsonPath().equals(facetColumnRequest.getJsonPath()), "Unexpected facet request jsonPath (Was '" + facetColumnRequest.getJsonPath() + "', Expected '" + subColumn.getJsonPath() + "')");
		}
		
		this.facetType = subColumn.getFacetType();
		this.columnName = columnName;
		this.facetColumnRequest = facetColumnRequest;
		this.columnTypeIsList = false;
		this.searchConditionString = createFacetSearchConditionString(facetColumnRequest, this.columnTypeIsList, subColumn.getColumnType());
		this.jsonPath = subColumn.getJsonPath();
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
	String getSearchConditionString(){
		return this.searchConditionString;
	}
	
	public FacetType getFacetType(){
		return this.facetType;
	}

	public boolean isColumnTypeIsList() {
		return columnTypeIsList;
	}
	
	public String getJsonPath() {
		return jsonPath;
	}
	
	static void validateFacetRequestType(FacetType facetType, FacetColumnRequest facetColumnRequest) {
		if (facetColumnRequest == null) {
			return;
		}
		
		if (FacetType.enumeration.equals(facetType) && !(facetColumnRequest instanceof FacetColumnValuesRequest)) {
			throw new IllegalArgumentException("facetColumnRequest was not an instance of FacetColumnValuesRequest");
		}
		
		if (FacetType.range.equals(facetType) && !(facetColumnRequest instanceof FacetColumnRangeRequest)) {
			throw new IllegalArgumentException("facetColumnRequest was not an instance of FacetColumnRangeRequest");
		}
	}

	/**
	 * Creates the search condition for a FacetColumnRequest
	 * @param facetColumnRequest
	 * @return the search condition string
	 */
	static String createFacetSearchConditionString(FacetColumnRequest facetColumnRequest, boolean columnTypeIsList, ColumnType jsonPathType){
		if (facetColumnRequest == null){
			return null;
		}
		
		if (facetColumnRequest instanceof FacetColumnValuesRequest){
			if(columnTypeIsList){
				return createListColumnEnumerationSearchCondition((FacetColumnValuesRequest) facetColumnRequest);
			}else {
				return createSingleValueColumnEnumerationSearchCondition((FacetColumnValuesRequest) facetColumnRequest, jsonPathType);
			}
		}else if (facetColumnRequest instanceof FacetColumnRangeRequest){
			return createRangeSearchCondition((FacetColumnRangeRequest) facetColumnRequest, jsonPathType);
		}else{
			throw new IllegalArgumentException("Unexpected instance of FacetColumnRequest");
		}
		
	}
	
	static String createRangeSearchCondition(FacetColumnRangeRequest facetRange, ColumnType jsonPathType){
		if( facetRange == null || ( StringUtils.isEmpty( facetRange.getMin() ) && StringUtils.isEmpty( facetRange.getMax() ) ) ){
			return null;
		}
		String min = facetRange.getMin();
		String max = facetRange.getMax();

		StringBuilder builder = new StringBuilder("(");

		appendColumnNameExpression(builder, facetRange);
		
		if (NULL_VALUE_KEYWORD.equals(min) || NULL_VALUE_KEYWORD.equals(max)){
			builder.append(" IS NULL");
		} else if(min == null){ //only max exists
			builder.append("<=");
			appendValueToStringBuilder(builder, max, jsonPathType);
		}else if (max == null){ //only min exists
			builder.append(">=");
			appendValueToStringBuilder(builder, min, jsonPathType);
		}else{
			builder.append(" BETWEEN ");
			appendValueToStringBuilder(builder, min, jsonPathType);
			builder.append(" AND ");
			appendValueToStringBuilder(builder, max, jsonPathType);
		}
		
		builder.append(")");
		return builder.toString();
	}
	
	static String createSingleValueColumnEnumerationSearchCondition(FacetColumnValuesRequest facetValues, ColumnType jsonPathType){
		if(facetValues == null || facetValues.getFacetValues() == null|| facetValues.getFacetValues().isEmpty()){
			return null;
		}
		
		StringBuilder builder = new StringBuilder("(");
		int initialSize = builder.length();
		for(String value : facetValues.getFacetValues()){
			if(builder.length() > initialSize){
				builder.append(" OR ");
			}
			
			appendColumnNameExpression(builder, facetValues);
			
			if(value.equals(NULL_VALUE_KEYWORD)){
				builder.append(" IS NULL");
			}else{
				builder.append("=");
				appendValueToStringBuilder(builder, value, jsonPathType);
			}
		}
		builder.append(")");
		return builder.toString();
	}
	
	static void appendColumnNameExpression(StringBuilder builder, FacetColumnRequest request) {
		// If a json path is supplied then we need to extract the value
		if (request.getJsonPath() != null) {
			builder.append("JSON_EXTRACT(");
		}
		
		builder.append(SqlElementUtils.wrapInDoubleQuotes(request.getColumnName()));
		
		if (request.getJsonPath() != null) {
			builder.append(",'").append(request.getJsonPath()).append("')");
		}
	}

	static String createListColumnEnumerationSearchCondition(FacetColumnValuesRequest facetValues){
		if(facetValues == null || facetValues.getFacetValues() == null|| facetValues.getFacetValues().isEmpty()){
			return null;
		}

		String quotedColumnName = SqlElementUtils.wrapInDoubleQuotes(facetValues.getColumnName());

		StringJoiner hasClauseJoiner = new StringJoiner(",", quotedColumnName + " HAS (", ")");
		//initial size will be non-zero because we gave the constructor a prefix and suffix
		int joinerInitialSize = hasClauseJoiner.length();

		boolean includeColumnIsNullCondition = false;
		for(String value : facetValues.getFacetValues()){
			// values inside lists may not have the null keyword (e.g. "[null]" is not allowed)
			// so seeing the null keyword is treated as selecting for columns in which there is no list value.
			if(value.equals(NULL_VALUE_KEYWORD)){
				includeColumnIsNullCondition = true;
			}else {
				hasClauseJoiner.add("'" + value.replaceAll("'", "''")+"'");
			}
		}

		String searchCondition;
		if(includeColumnIsNullCondition){
			boolean noValuesAddedToJoiner = hasClauseJoiner.length() == joinerInitialSize;
			String isNullCondition = quotedColumnName + " IS NULL";
			if(noValuesAddedToJoiner){
				searchCondition = isNullCondition ;
			}else{
				searchCondition = hasClauseJoiner + " OR " + isNullCondition;
			}
		} else {
			searchCondition = hasClauseJoiner.toString();
		}

		return "(" + searchCondition + ")";
	}

	/**
	 * Appends a value to the string builder
	 * and places single quotes (') around it if the column type is String
	 */ 
	static void appendValueToStringBuilder(StringBuilder builder, String value, ColumnType targetType){
		if (targetType != null) {
			builder.append("CAST(");
		}
		builder.append("'");
		builder.append(value.replaceAll("'", "''"));
		builder.append("'");
		if (targetType != null) {
			builder.append(" AS ").append(targetType.name()).append(")");
		}
	}


}
