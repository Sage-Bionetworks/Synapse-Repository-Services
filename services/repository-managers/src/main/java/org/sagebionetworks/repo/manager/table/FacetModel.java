package org.sagebionetworks.repo.manager.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.JsonSubColumnModel;
import org.sagebionetworks.table.cluster.TranslationDependencies;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.model.TableName;
import org.sagebionetworks.table.query.util.FacetRequestColumnModel;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Class responsible for generating all facet related queries and transforming
 * the query results into FacetColumnResult.
 * @author zdong
 *
 */
public class FacetModel {
	
	private List<FacetRequestColumnModel> validatedFacets;
	private List<FacetTransformer> facetTransformers;
	
	
	/**
	 * Constructor
	 * @param selectedFacets list of facets filters selected by the user
	 * @param sqlQuery the sqlQuery on which to base the generated facet queries.
	 * @param returnFacets whether facet information will be returned back to the user
	 */
	public FacetModel(List<FacetColumnRequest> selectedFacets, QueryExpression originalQuery, TranslationDependencies dependencies, boolean returnFacets) {
		ValidateArgument.required(originalQuery, "originalQuery");
		IdAndVersion id = IdAndVersion.parse(originalQuery.getFirstElementOfType(TableName.class).toSql());
		List<ColumnModel> tableSchema = dependencies.getSchemaProvider().getTableSchema(id);
		this.validatedFacets = createValidatedFacetsList(selectedFacets, tableSchema, returnFacets);
		this.facetTransformers = generateFacetQueryTransformers(originalQuery, dependencies, this.validatedFacets);
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
	

	static List<FacetRequestColumnModel> createValidatedFacetsList(List<FacetColumnRequest> selectedFacets, List<ColumnModel> schema,
			boolean returnFacets) {
		ValidateArgument.required(schema, "schema");
		
		Map<String, FacetColumnRequest> selectedFacetMap = createColumnNameToFacetColumnMap(selectedFacets);

		//keeps track of all faceted column names to verify user does not ask for filtering of an unfaceted column name
		Set<String> facetedColumnNames = new HashSet<>();
		//create the SearchConditions based on each facet column's values and store them into the list
		List <FacetRequestColumnModel> validatedFacetsList = new ArrayList<FacetRequestColumnModel>();
		for(ColumnModel columnModel : schema){
			FacetColumnRequest facetColumnRequest = selectedFacetMap.get(getColumnNameKey(columnModel.getName(), null));

			processFacetColumnRequest(validatedFacetsList, facetedColumnNames, columnModel, null, facetColumnRequest, returnFacets);
			
			if (columnModel.getJsonSubColumns() != null) {
				columnModel.getJsonSubColumns().forEach(subColumn -> {
					FacetColumnRequest subColumnFacetColumnRequest = selectedFacetMap.get(getColumnNameKey(columnModel.getName(), subColumn.getJsonPath()));
			
					processFacetColumnRequest(validatedFacetsList, facetedColumnNames, columnModel, subColumn, subColumnFacetColumnRequest, returnFacets);
				});
			}
		}
		
		if(!facetedColumnNames.containsAll(selectedFacetMap.keySet())){
				throw new InvalidTableQueryFacetColumnRequestException("Requested facet column names must all be in the set: " + facetedColumnNames + " Requested set of column names: " + selectedFacets);
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
	static void processFacetColumnRequest(List<FacetRequestColumnModel> validatedFacetsList, Set<String> facetedColumnNames,
			ColumnModel columnModel, JsonSubColumnModel subColumn, FacetColumnRequest facetColumnRequest, boolean returnFacets) {
		if(columnModel.getFacetType() != null) {
			facetedColumnNames.add(getColumnNameKey(columnModel.getName(), null));
			
			//if it is a faceted column and user either wants returned facets or they have applied a filter to the facet
			if (returnFacets || facetColumnRequest != null) {
				validatedFacetsList.add(new FacetRequestColumnModel(columnModel, facetColumnRequest));
			}
		} else if (subColumn != null && subColumn.getFacetType() != null) {
			
			facetedColumnNames.add(getColumnNameKey(columnModel.getName(), subColumn.getJsonPath()));
			
			if (returnFacets || facetColumnRequest != null) {
				validatedFacetsList.add(new FacetRequestColumnModel(columnModel.getName(), subColumn, facetColumnRequest));
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
				FacetColumnRequest shouldBeNull = result.put(getColumnNameKey(facet.getColumnName(), facet.getJsonPath()), facet);
				if(shouldBeNull != null){
					throw new IllegalArgumentException("Request contains QueryRequestFacetColumn with a duplicate column name");
				}
			}
		}
		return result;
	}
	
	static List<FacetTransformer> generateFacetQueryTransformers(QueryExpression originalQuery, TranslationDependencies dependencies, List<FacetRequestColumnModel> validatedFacets){
		ValidateArgument.required(originalQuery, "originalQuery");
		ValidateArgument.required(dependencies, "dependencies");
		ValidateArgument.required(validatedFacets, "validatedFacets");
		
		List<FacetTransformer> transformersList = new ArrayList<>(validatedFacets.size());
		for(FacetRequestColumnModel facet: validatedFacets) {
			QueryExpression queryClone = cloneQuery(originalQuery);
			switch(facet.getFacetType()) {
				case enumeration:
					Set<String> selectedValues = null;
					FacetColumnValuesRequest facetValuesRequest = (FacetColumnValuesRequest) facet.getFacetColumnRequest();
					if ( facetValuesRequest != null){
						selectedValues = facetValuesRequest.getFacetValues();
					}
					transformersList.add(new FacetTransformerValueCounts(facet.getColumnName(), facet.getJsonPath(), facet.isColumnTypeIsList(), validatedFacets, queryClone , dependencies, selectedValues));
					break;
				case range:
					String selectedMin = null;
					String selectedMax = null;
					FacetColumnRangeRequest facetRangeRequest = (FacetColumnRangeRequest) facet.getFacetColumnRequest();
					if ( facetRangeRequest != null){
						selectedMin = facetRangeRequest.getMin();
						selectedMax = facetRangeRequest.getMax();
					}
					transformersList.add(new FacetTransformerRange(facet.getColumnName(), facet.getJsonPath(), validatedFacets, queryClone, dependencies, selectedMin, selectedMax ));
					break;
				default:
					throw new RuntimeException("Found unexpected FacetType");
			}
		}
		return transformersList;
	}
	
	private static String getColumnNameKey(String columnName, String jsonPath) {
		if (jsonPath == null) {
			return columnName;
		}
		return columnName + "." + jsonPath;
	}
	
	private static QueryExpression cloneQuery(QueryExpression toClone) {
		try {
			return new TableQueryParser(toClone.toSql()).queryExpression();
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
}
