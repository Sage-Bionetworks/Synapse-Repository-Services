package org.sagebionetworks.repo.manager.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.WhereClause;
import org.sagebionetworks.table.query.util.TableSqlProcessor;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Class responsible for generating all facet related queries and transforming
 * the query results into FacetColumnResult.
 * @author zdong
 *
 */
public class FacetModel {
	public static final Long MAX_NUM_FACET_CATEGORIES = 100L;
	
	private List<ValidatedQueryFacetColumn> validatedFacets;
	private boolean hasFilters;
	private SqlQuery facetedQuery;
	private List<FacetTransformer> facetTransformers;
	
	
	/**
	 * Constructor
	 * @param selectedFacets list of facets filters selected by the user
	 * @param sqlQuery the sqlQuery on which to base the generated facet queries.
	 * @param returnFacets whether facet information will be returned back to the user
	 */
	public FacetModel(List<FacetColumnRequest> selectedFacets, SqlQuery sqlQuery, boolean returnFacets) {
		ValidateArgument.required(sqlQuery, "sqlQuery");
	
		//setting fields
		this.hasFilters = selectedFacets != null && !selectedFacets.isEmpty();
		
		this.validatedFacets = createValidatedFacetsList(selectedFacets, sqlQuery.getTableSchema(), returnFacets);
		
		//generate the faceted query and facet transformers
		this.facetedQuery = generateFacetFilteredQuery(sqlQuery, this.validatedFacets);
		this.facetTransformers = generateFacetQueryTransformers(sqlQuery, this.validatedFacets);
	}
	
	/**
	 * Returns whether there were facet filters selected
	 * @return
	 */
	public boolean hasFiltersApplied(){
		return hasFilters;
	}
	
	/**
	 * Returns a new SqlQuery with searchConditions derived from the facetColums list appended to the query's WhereClause
	 * @return
	 */
	public SqlQuery getFacetFilteredQuery(){
		return this.facetedQuery;
	}
	
	/**
	 * Returns a list of FacetTransformers which contains a method to get a
	 * sql query that calculates facet information for each faceted column
	 * and a method to convert that query's result into an FacetColumnResult. 
	 * The order of the list matches that of the table schema.
	 * @return
	 */
	public List<FacetTransformer> getFacetInformationQueries(){
		return this.facetTransformers;
	}
	

	static List<ValidatedQueryFacetColumn> createValidatedFacetsList(List<FacetColumnRequest> selectedFacets, List<ColumnModel> schema,
			boolean returnFacets) {
		ValidateArgument.required(schema, "schema");
		
		Map<String, FacetColumnRequest> selectedFacetMap = createColumnNameToFacetColumnMap(selectedFacets);

		//keeps track of all faceted column names to verify user does not ask for filtering of an unfaceted column name
		Set<String> facetedColumnNames = new HashSet<>();
		//create the SearchConditions based on each facet column's values and store them into the list
		List <ValidatedQueryFacetColumn> validatedFacetsList = new ArrayList<ValidatedQueryFacetColumn>();
		for(ColumnModel columnModel : schema){
			FacetColumnRequest facetColumnRequest = selectedFacetMap.get(columnModel.getName());

			processFacetColumnRequest(validatedFacetsList, facetedColumnNames, columnModel, facetColumnRequest, returnFacets);
		}
		
		if(!facetedColumnNames.containsAll(selectedFacetMap.keySet())){
			throw new IllegalArgumentException("Requested facet column names must all be in the set: " + facetedColumnNames + " Requested set of column names: " + selectedFacets);
		}
		
		return validatedFacetsList;
	}
	
	/**
	 * Determines whether or not to add a facet to the validatedFacetsList and facetedColumnNames 
	 * @param returnFacets
	 * @param facetedColumnNames
	 * @param validatedFacetsList
	 * @param columnModel
	 * @param facetColumnRequest
	 */
	static void processFacetColumnRequest(List<ValidatedQueryFacetColumn> validatedFacetsList, Set<String> facetedColumnNames,
			ColumnModel columnModel, FacetColumnRequest facetColumnRequest, boolean returnFacets) {
		if(columnModel.getFacetType() != null){
			facetedColumnNames.add(columnModel.getName());
			
			//if it is a faceted column and user either wants returned facets or they have applied a filter to the facet
			if (returnFacets || facetColumnRequest != null ){
				validatedFacetsList.add(new ValidatedQueryFacetColumn(columnModel, facetColumnRequest));
			}
		}
	}
	
	/**
	 * Returns a Map where the key is the name of a facet and the value is the corresponding QueryRequestFacetColumn
	 * @return
	 */
	static Map<String, FacetColumnRequest> createColumnNameToFacetColumnMap(List<FacetColumnRequest> selectedFacets){
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
	
	static SqlQuery generateFacetFilteredQuery(SqlQuery sqlQuery, List<ValidatedQueryFacetColumn> validatedFacets){
		ValidateArgument.required(sqlQuery, "sqlQuery");
		ValidateArgument.required(validatedFacets, "validatedFacets");
		try{
			QuerySpecification modelCopy = new TableQueryParser(sqlQuery.getModel().toSql()).querySpecification();
			if(!validatedFacets.isEmpty()){
				WhereClause originalWhereClause = sqlQuery.getModel().getTableExpression().getWhereClause();
				
				String facetSearchConditionString = FacetUtils.concatFacetSearchConditionStrings(validatedFacets, null);
				
				StringBuilder builder = new StringBuilder();
				TableSqlProcessor.appendFacetWhereClauseToStringBuilderIfNecessary(builder, facetSearchConditionString, originalWhereClause);
				
				// create the new where if necessary
				if(builder.length() > 0){
					WhereClause newWhereClause = new TableQueryParser(builder.toString()).whereClause();
					modelCopy.getTableExpression().replaceWhere(newWhereClause);
				}
			}
			return new SqlQuery(modelCopy, sqlQuery);
		}catch (ParseException e){
			throw new RuntimeException(e);
		}
	}
	
	static List<FacetTransformer> generateFacetQueryTransformers(SqlQuery sqlQuery, List<ValidatedQueryFacetColumn> validatedFacets){
		ValidateArgument.required(sqlQuery, "sqlQuery");
		ValidateArgument.required(validatedFacets, "validatedFacets");
		
		List<FacetTransformer> transformersList = new ArrayList<>(validatedFacets.size());
		for(ValidatedQueryFacetColumn facet: validatedFacets){
			switch(facet.getFacetType()){
				case enumeration:
					Set<String> selectedValues = null;
					FacetColumnValuesRequest facetValuesRequest = (FacetColumnValuesRequest) facet.getFacetColumnRequest();
					if ( facetValuesRequest != null){
						selectedValues = facetValuesRequest.getFacetValues();
					}
					transformersList.add(new FacetTransformerValueCounts(facet.getColumnName(), validatedFacets, sqlQuery, selectedValues));
					break;
				case range:
					String selectedMin = null;
					String selectedMax = null;
					FacetColumnRangeRequest facetRangeRequest = (FacetColumnRangeRequest) facet.getFacetColumnRequest();
					if ( facetRangeRequest != null){
						selectedMin = facetRangeRequest.getMin();
						selectedMax = facetRangeRequest.getMax();
					}
					transformersList.add(new FacetTransformerRange(facet.getColumnName(), validatedFacets, sqlQuery, selectedMin, selectedMax ));
					break;
				default:
					throw new RuntimeException("Found unexpected FacetType");
			}
		}
		return transformersList;
	}
}
