package org.sagebionetworks.table.query.util;

import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.WhereClause;
import org.sagebionetworks.util.ValidateArgument;

public class FacetUtils {
		
	/**
	 * Concatenates a list of search condition Strings with AND and then wraps that search condition with a parenthesis
	 * e.g. ["(col1 = 1 OR col1 = b)", "(col2 = c OR col2 = d)"] => "( (col1 = 1 OR col1 = b) AND (col2 = c OR col2 = d) )"
	 * @param columNameExpressionToIgnore the result of {@link #getColumnNameExpression(String, String)} to exclude from the concatenation
	 * @return the concatenated string or null if there was nothing to concatenate
	 */
	public static String concatFacetSearchConditionStrings(List<FacetRequestColumnModel> validatedFacets, String columNameExpressionToIgnore){
		ValidateArgument.required(validatedFacets, "validatedFacets");
		
		if(validatedFacets.isEmpty()){
			return null;
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append("(");
		int initialSize = builder.length(); //length with the "( " included
		
		for(FacetRequestColumnModel facetColumn : validatedFacets){
			if (columNameExpressionToIgnore != null) {
				String columnNameExpression = getColumnNameExpression(facetColumn.getColumnName(), facetColumn.getJsonPath(), facetColumn.getJsonPathType());				
				//make sure not to include the ignored column
				if (columnNameExpression.equals(columNameExpressionToIgnore)) {
					continue;
				}
			} 
			String searchCondition = facetColumn.getSearchConditionString();
			if(searchCondition != null){//don't add anything if there is no search condition
				if(builder.length() > initialSize){ //not the first element
					builder.append(" AND ");
				}
				builder.append(searchCondition);
			}
		}
		if(builder.length() == initialSize){ //edge case where nothing got concatenated together
			return null;
		}
		
		builder.append(")");
		return builder.toString();
	}

	/**
	 * Returns a new QuerySpecification object that is modified version of 
	 * sqlModel having the original WHERE clause ANDed with 
	 * the facet search conditions in facetRequestColumnModels
	 * @param sqlModel
	 * @param facetRequestColumnModels
	 * @throws ParseException
	 */
	public static WhereClause appendFacetSearchConditionToQuerySpecification(WhereClause originalWhereClause,
			List<FacetRequestColumnModel> facetRequestColumnModels) throws ParseException {
		if (!facetRequestColumnModels.isEmpty()) {
			String facetSearchConditionString = FacetUtils.concatFacetSearchConditionStrings(facetRequestColumnModels,
					null);

			StringBuilder builder = new StringBuilder();
			SqlElementUtils.appendCombinedWhereClauseToStringBuilder(builder, facetSearchConditionString,
					originalWhereClause);

			// create the new where if necessary
			if (builder.length() > 0) {
				return new TableQueryParser(builder.toString()).whereClause();
			}
		}
		return originalWhereClause;
	}
	
	public static String getColumnNameExpression(String columnName, String jsonPath, ColumnType jsonPathType) {
		ValidateArgument.required(columnName, "The columnName");
		ValidateArgument.requirement(jsonPath == null || jsonPathType != null, "When the jsonPath is supplied the columnType is required.");
		
		String columnExpression = SqlElementUtils.wrapInDoubleQuotes(columnName);
		
		// If a json path is supplied then we need to extract the value
		if (jsonPath != null) {
			
			columnExpression = "JSON_EXTRACT(" + columnExpression + ",'" + jsonPath + "')";
			
			// We only unquote String types for now since this returns a utf string and aggregations might now work (See https://sagebionetworks.jira.com/browse/PLFM-8045)
			if (ColumnType.STRING.equals(jsonPathType)) {
				columnExpression = "JSON_UNQUOTE(" + columnExpression + ")";
			}
		}
		
		return columnExpression;
	}
}
