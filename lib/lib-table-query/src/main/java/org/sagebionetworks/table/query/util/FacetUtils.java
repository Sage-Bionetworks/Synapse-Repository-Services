package org.sagebionetworks.table.query.util;

import java.util.List;

import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.WhereClause;
import org.sagebionetworks.util.ValidateArgument;

public class FacetUtils {
	/**
	 * Concatenates a list of search condition Strings with AND and then wraps that search condition with a parenthesis
	 * e.g. ["(col1 = 1 OR col1 = b)", "(col2 = c OR col2 = d)"] => "( (col1 = 1 OR col1 = b) AND (col2 = c OR col2 = d) )"
	 * @param columNameToIgnore the name of the column to exclude from the concatenation
	 * @return the concatenated string or null if there was nothing to concatenate
	 */
	public static String concatFacetSearchConditionStrings(List<FacetRequestColumnModel> validatedFacets, String columNameToIgnore){
		ValidateArgument.required(validatedFacets, "validatedFacets");
		
		if(validatedFacets.isEmpty()){
			return null;
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append("(");
		int initialSize = builder.length(); //length with the "( " included
		
		for(FacetRequestColumnModel facetColumn : validatedFacets){
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
				if(facetSearchConditionString != null){
					builder.append("(");
				}
				builder.append(originalWhereClause.getSearchCondition().toSql());
				if(facetSearchConditionString != null){
					builder.append(")");
				}
			}
			if(facetSearchConditionString != null){
				if(originalWhereClause != null){
					builder.append(" AND ");
					builder.append("(");
				}
				builder.append(facetSearchConditionString);
				if(originalWhereClause != null){
					builder.append(")");
				}
			}
		}
	}
	
	public static QuerySpecification appendWhereClauseToQuerySpecification(QuerySpecification sqlModel, List<FacetRequestColumnModel> validatedFacets) throws ParseException{
		QuerySpecification modelCopy = new TableQueryParser(sqlModel.toSql()).querySpecification();
		if(!validatedFacets.isEmpty()){
			WhereClause originalWhereClause = sqlModel.getTableExpression().getWhereClause();
			
			String facetSearchConditionString = FacetUtils.concatFacetSearchConditionStrings(validatedFacets, null);
			
			StringBuilder builder = new StringBuilder();
			FacetUtils.appendFacetWhereClauseToStringBuilderIfNecessary(builder, facetSearchConditionString, originalWhereClause);
			
			// create the new where if necessary
			if(builder.length() > 0){
				WhereClause newWhereClause = new TableQueryParser(builder.toString()).whereClause();
				modelCopy.getTableExpression().replaceWhere(newWhereClause);
			}
		}
		return modelCopy;
	}
}
