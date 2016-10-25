package org.sagebionetworks.repo.manager.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.WhereClause;
import org.sagebionetworks.util.ValidateArgument;

public class FacetModel {
	public static final Long MAX_NUM_FACET_CATEGORIES = 100L;
	
	private List<ValidatedQueryFacetColumn> validatedFacets;
	private boolean hasFilters;
	private SqlQuery sqlQuery;
	
	//these are cached
	private SqlQuery facetedQuery = null;
	private List<FacetTransformer> facetTransformers = null;
	
	
	public FacetModel(List<FacetColumnRequest> selectedFacets, SqlQuery sqlQuery, boolean returnFacets) {
		ValidateArgument.required(selectedFacets, "selectedFacets");
		ValidateArgument.required(sqlQuery, "schema");
		
		//process the selected facets
		Map<String, FacetColumnRequest> selectedFacetMap = createColumnNameToFacetColumnMap(selectedFacets);
		
		//check that there are no unsupported facet column names selected
		if(!sqlQuery.getColumnNameToModelMap().keySet().containsAll(selectedFacetMap.keySet())){
			throw new IllegalArgumentException("Requested facet column names must be in the set:" + sqlQuery.getColumnNameToModelMap().keySet().toString());
		}
		
		//setting fields
		this.validatedFacets = new ArrayList<ValidatedQueryFacetColumn>();
		this.hasFilters = selectedFacets != null && !selectedFacets.isEmpty();
		this.sqlQuery = sqlQuery;
		
		//create the SearchConditions based on each facet column's values and store them into facetSearchConditionStrings
		for(ColumnModel columnModel : sqlQuery.getTableSchema()){
			if(columnModel.getFacetType() != null){				
				FacetColumnRequest facetColumnRequest = selectedFacetMap.get(columnModel.getName());
				
				//if it is a faceted column and user either wants returned facets or they have applied a filter to the facet
				if ((returnFacets || facetColumnRequest != null) ){
					this.validatedFacets.add(new ValidatedQueryFacetColumn(columnModel.getName(), columnModel.getFacetType(), facetColumnRequest));
				}
			}
			
		}
		
		
	}
	
	public boolean hasFiltersApplied(){
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
	public SqlQuery getFacetFilteredQuery(){
		//if it's already been generated just return that
		if(facetedQuery != null) return this.facetedQuery;
		
		try{
			QuerySpecification modelCopy = new TableQueryParser(sqlQuery.getModel().toSql()).querySpecification();
			if(!this.validatedFacets.isEmpty()){
				WhereClause originalWhereClause = sqlQuery.getModel().getTableExpression().getWhereClause();
				
				String facetSearchConditionString = FacetUtils.concatFacetSearchConditionStrings(null, this.validatedFacets);
				
				StringBuilder builder = new StringBuilder();
				FacetUtils.appendFacetWhereClauseToStringBuilderIfNecessary(builder, facetSearchConditionString, originalWhereClause);
				
				// create the new where if necessary
				if(builder.length() > 0){
					WhereClause newWhereClause = new TableQueryParser(builder.toString()).whereClause();
					modelCopy.getTableExpression().replaceWhere(newWhereClause);
				}
			}
			
			//save the faceted query because it wont change next time;
			this.facetedQuery =  new SqlQuery(modelCopy, sqlQuery);
			return this.facetedQuery;
		}catch (ParseException e){
			throw new RuntimeException(e);
		}
		
	}
	
	public List<FacetTransformer> getFacetInformationQueries(){
		//if it's already been generated just return that
		if (this.facetTransformers != null) return this.facetTransformers;
		
		List<FacetTransformer> transformersList = new ArrayList<>(validatedFacets.size());
		for(ValidatedQueryFacetColumn facet: validatedFacets){
			switch(facet.getFacetType()){
				case enumeration:
					Set<String> selectedValues = null;
					FacetColumnRequest facetRequest = facet.getFacetColumnRequest();
					if ( facetRequest != null){
						selectedValues = ((FacetColumnValuesRequest) facetRequest).getFacetValues();
					}
					transformersList.add(new FacetTransformerValueCounts(facet.getColumnName(), validatedFacets, sqlQuery, selectedValues));
					break;
				case range:
					transformersList.add(new FacetTransformerRange(facet.getColumnName(), validatedFacets, sqlQuery));
					break;
				default:
					throw new RuntimeException("Found unexpected FacetType");
			}
		}
		this.facetTransformers = transformersList;
		return this.facetTransformers;
	}
}
