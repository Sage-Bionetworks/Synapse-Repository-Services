package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.util.ValidateArgument;

public class FacetUtils {
	/**
	 * Concatenates a list of search condition Strings with AND and then wraps that search condition with a parenthesis
	 * e.g. ["(col1 = 1 OR col1 = b)", "(col2 = c OR col2 = d)"] => "( (col1 = 1 OR col1 = b) AND (col2 = c OR col2 = d) )"
	 * @param columNameToIgnore the name of the column to exclude from the concatenation
	 * @return the concatenated string or null if there was nothing to concatenate
	 */
	public static String concatFacetSearchConditionStrings(List<ValidatedQueryFacetColumn> validatedFacets, String columNameToIgnore){
		ValidateArgument.required(validatedFacets, "validatedFacets");
		
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
}
