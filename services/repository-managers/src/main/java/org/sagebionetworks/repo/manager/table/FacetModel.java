package org.sagebionetworks.repo.manager.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.table.cluster.SQLTranslatorUtils;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.Pagination;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.TableExpression;
import org.sagebionetworks.table.query.model.WhereClause;
import org.sagebionetworks.util.ValidateArgument;

public class FacetModel {
	public static final Long MAX_NUM_FACET_CATEGORIES = 100L;
	public static final String VALUE_ALIAS = "value";
	public static final String COUNT_ALIAS = "frequency";
	public static final String MIN_ALIAS = "minimum";
	public static final String MAX_ALIAS = "maximum";
	
	
	private List<ValidatedQueryFacetColumn> validatedFacets;
	private boolean hasFilters;
	
	
	public FacetModel(List<FacetColumnRequest> selectedFacets, Map<String,ColumnModel> schema, boolean returnFacets) {
		ValidateArgument.required(selectedFacets, "selectedFacets");
		ValidateArgument.required(schema, "schema");
		
		//process the selected facets
		
		Map<String, FacetColumnRequest> selectedFacetMap = createColumnNameToFacetColumnMap(selectedFacets);
		
		//check that there are no unsupported facet column names selected
		if(!schema.keySet().containsAll(selectedFacetMap.keySet())){
			throw new IllegalArgumentException("Requested facet column names must be in the set:" + schema.keySet().toString());
		}
		this.validatedFacets = new ArrayList<ValidatedQueryFacetColumn>();
		this.hasFilters = selectedFacets != null && !selectedFacets.isEmpty();
		
		
		//create the SearchConditions based on each facet column's values and store them into facetSearchConditionStrings
		for(ColumnModel columnModel : schema.values()){
			if(columnModel.getFacetType() != null){				
				FacetColumnRequest facetColumnRequest = selectedFacetMap.get(columnModel.getName());
				
				//if it is a faceted column and user either wants returned facets or they have applied a filter to the facet
				if ((returnFacets || facetColumnRequest != null) ){
					this.validatedFacets.add(new ValidatedQueryFacetColumn(columnModel.getName(), columnModel.getFacetType(), facetColumnRequest));
				}
			}
			
		}
		
		
	}
	
	public boolean hasFilters(){
		return hasFilters;
	}
	
	/**
	 * Returns a Map where the key is the name of a facet and the value is the corresponding QueryRequestFacetColumn
	 * @return
	 */
	public static Map<String, FacetColumnRequest> createColumnNameToFacetColumnMap(List<FacetColumnRequest> selectedFacets){
		Map<String, FacetColumnRequest> result = new HashMap<String, FacetColumnRequest>();
		if(selectedFacets != null){
			for(FacetColumnRequest facet : selectedFacets){
				FacetColumnRequest shouldBeNull = result.put(facet.getColumnName(), facet);
				if(shouldBeNull != null){
					throw new IllegalArgumentException("Request contains QueryRequestFacetColumn with a duplicate column name");
				}
			}
		}
		return result;
	}
	
	/**
	 * Returns a copy of the SqlQuery with searchConditions derived from the facetColums list appended to the query's WhereClause
	 * @param query
	 * @param facetColumns
	 * @return
	 */
	public SqlQuery getFacetFilteredQuery(SqlQuery query){
		ValidateArgument.required(query, "query");
		
		try{
			QuerySpecification modelCopy = new TableQueryParser(query.getModel().toSql()).querySpecification();
			if(!this.validatedFacets.isEmpty()){
				WhereClause originalWhereClause = query.getModel().getTableExpression().getWhereClause();
				
				String facetSearchConditionString = concatFacetSearchConditionStrings(null);
				
				StringBuilder builder = new StringBuilder();
				appendFacetWhereClauseToStringBuilderIfNecessary(builder, facetSearchConditionString, originalWhereClause);
				
				// create the new where if necessary
				if(builder.length() > 0){
					WhereClause newWhereClause = new TableQueryParser(builder.toString()).whereClause();
					modelCopy.getTableExpression().replaceWhere(newWhereClause);
				}
			}
			// return a copy
			return new SqlQuery(modelCopy, query);
		}catch (ParseException e){
			throw new RuntimeException(e);
		}
		
	}
	
	/**
	 * Concatenates a list of search condition Strings with AND and then wraps that search condition with a parenthesis
	 * e.g. ["(col1 = 1 OR col1 = b)", "(col2 = c OR col2 = d)"] => "( (col1 = 1 OR col1 = b) AND (col2 = c OR col2 = d) )"
	 * @param columNameToIgnore the name of the column to exclude from the concatenation
	 * @return the concatenated string or null if there was nothing to concatenate
	 */
	public String concatFacetSearchConditionStrings(String columNameToIgnore){
		
		if(validatedFacets.isEmpty()){
			return null;
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append("(");
		int initialSize = builder.length(); //length with the "( " included
		
		for(ValidatedQueryFacetColumn facetColumn : validatedFacets){
			if(columNameToIgnore == null || !facetColumn.getColumnName().equals(columNameToIgnore)){ //make sure not to include the ignored column
				String searchCondition = facetColumn.getSearchConditionString();
				if(searchCondition != null){//don't add anything if there is no search condition
					if(builder.length() > initialSize){ //not the first element
						builder.append(" AND ");
					}
					builder.append(searchCondition);
				}
			}
		}
		if(builder.length() == initialSize){ //edge case where nothing got concatenated together
			return null;
		}
		
		builder.append(")");
		return builder.toString();
	}
	
	/**
	 * Creates a query that gets facet counts for its most frequent values in the specified column based on the original query.
	 */
	//TODO: maybe return pair of query specification and RowMapper????
	public QuerySpecification getFacetColumnCountQuery(SqlQuery originalQuery, String columnName){
		ValidateArgument.required(originalQuery, "originalQuery");
		ValidateArgument.required(columnName, "columnName");
		
		QuerySpecification facetColumnCountQuery = createFilteredFacetCountSqlQuerySpecification(columnName, originalQuery.getModel());
		Map<String, Object> parameters = new HashMap<>();
		SQLTranslatorUtils.translateModel(facetColumnCountQuery, parameters, originalQuery.getColumnNameToModelMap());
		
		return facetColumnCountQuery;
	}
	
	//TODO: maybe return pair of query specification and RowMapper????
	public QuerySpecification getFacetColumnRangeQuery(SqlQuery originalQuery, String columnName){
		ValidateArgument.required(originalQuery, "originalQuery");
		ValidateArgument.required(columnName, "columnName");
		
		QuerySpecification facetColumnRangeQuery = createFilteredFacetRangeSqlQuerySpecification(columnName, originalQuery.getModel());
		Map<String, Object> parameters = new HashMap<>();
		SQLTranslatorUtils.translateModel(facetColumnRangeQuery, parameters, originalQuery.getColumnNameToModelMap());
		
		return facetColumnRangeQuery;
	}
	
	/**
	 * Creates a SQL query for finding the counts of each distinct value in a faceted column
	 * @param columnName the name of the faceted column
	 * @param model the original (non-transformed) query off of which to obtain the FROM and WHERE clauses
	 * @param facetSearchConditionString
	 * @return the generated SQL query represented by QuerySpecification
	 */
	public QuerySpecification createFilteredFacetCountSqlQuerySpecification(String columnName, QuerySpecification model){
		ValidateArgument.required(columnName, "columnName");
		ValidateArgument.required(model, "model");
		
		String facetSearchConditionString = concatFacetSearchConditionStrings(columnName);
		
		TableExpression tableExpressionFromModel = model.getTableExpression();
		Pagination pagination = new Pagination(MAX_NUM_FACET_CATEGORIES, null);//TODO: move this later to when we figure out the limit thing
		StringBuilder builder = new StringBuilder("SELECT ");
		builder.append(columnName);
		builder.append(" AS ");
		builder.append(VALUE_ALIAS);
		builder.append(", COUNT(*) AS ");
		builder.append(COUNT_ALIAS);
		builder.append(" ");
		builder.append(tableExpressionFromModel.getFromClause().toSql());
		appendFacetWhereClauseToStringBuilderIfNecessary(builder, facetSearchConditionString, tableExpressionFromModel.getWhereClause());
		builder.append(" GROUP BY " + columnName + " ");
		builder.append(pagination.toSql());
		
		try {
			return new TableQueryParser(builder.toString()).querySpecification();
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Creates a SQL query for finding the minimum and maximum values of a faceted column
	 * @param columnName the name of the faceted column
	 * @param model model the original (non-transformed) query off of which to obtain the FROM and WHERE clauses
	 * @param facetSearchConditionString
	 * @return the generated SQL query represented by QuerySpecification
	 */
	public QuerySpecification createFilteredFacetRangeSqlQuerySpecification(String columnName, QuerySpecification model){
		ValidateArgument.required(columnName, "columnName");
		ValidateArgument.required(model, "model");
		
		String facetSearchConditionString = concatFacetSearchConditionStrings(columnName);
		
		TableExpression tableExpressionFromModel = model.getTableExpression();
		StringBuilder builder = new StringBuilder("SELECT MIN(");
		builder.append(columnName);
		builder.append(") as ");
		builder.append(MIN_ALIAS);
		builder.append(", MAX(");
		builder.append(columnName);
		builder.append(") as ");
		builder.append(MAX_ALIAS);
		builder.append(" ");
		builder.append(model.getTableExpression().getFromClause().toSql());
		appendFacetWhereClauseToStringBuilderIfNecessary(builder, facetSearchConditionString, tableExpressionFromModel.getWhereClause());
		
		try {
			return new TableQueryParser(builder.toString()).querySpecification();
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Appends a WHERE clause to the String Builder if necessary
	 * @param builder StringBuilder to append to
	 * @param facetSearchConditionString SearchCondition string to append. pass null if none to append.
	 * @param originalWhereClause the WHERE clause that was in the original query. pass null if not exist.
	 */
	public static void appendFacetWhereClauseToStringBuilderIfNecessary(StringBuilder builder, String facetSearchConditionString,
			WhereClause originalWhereClause) {
		ValidateArgument.required(builder, "builder");
		
		if(facetSearchConditionString != null || originalWhereClause != null){
			builder.append(" WHERE ");
			if(originalWhereClause != null){
				builder.append("(");
				builder.append(originalWhereClause.getSearchCondition().toSql());
				builder.append(")");
			}
			if(facetSearchConditionString != null){
				if(originalWhereClause != null){
					builder.append(" AND ");
				}
				builder.append("(");
				builder.append(facetSearchConditionString);
				builder.append(")");
			}
		}
	}
}
