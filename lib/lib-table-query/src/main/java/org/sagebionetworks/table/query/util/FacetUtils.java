package org.sagebionetworks.table.query.util;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.table.ColumnMultiValueFunctionQueryFilter;
import org.sagebionetworks.repo.model.table.ColumnSingleValueQueryFilter;
import org.sagebionetworks.repo.model.table.QueryFilter;
import org.sagebionetworks.repo.model.table.TextMatchesQueryFilter;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.TextMatchesPredicate;
import org.sagebionetworks.table.query.model.WhereClause;
import org.sagebionetworks.util.ValidateArgument;

public class FacetUtils {
	/**
	 * Concatenates a list of search condition Strings with AND and then wraps that search condition with a parenthesis
	 * e.g. ["(col1 = 1 OR col1 = b)", "(col2 = c OR col2 = d)"] => "( (col1 = 1 OR col1 = b) AND (col2 = c OR col2 = d) )"
	 * @param columNameToIgnore the name of the column to exclude from the concatenation
	 * @return the concatenated string or null if there was nothing to concatenate
	 */
	public static String concatFacetSearchConditionStrings(List<FacetRequestColumnModel> validatedFacets, Optional<String> columNameToIgnore){
		ValidateArgument.required(validatedFacets, "validatedFacets");
		
		if(validatedFacets.isEmpty()){
			return null;
		}

		List<FacetRequestColumnModel> facetColumnsWithoutIgnoredColumn = validatedFacets.stream()
				.filter(facet -> {
					if (columNameToIgnore.isPresent()) {
						return !facet.getColumnName().equals(columNameToIgnore.get());
					} else {
						return true;
					}
				})
				.collect(Collectors.toList());

		List<String> conditionStrings = facetColumnsWithoutIgnoredColumn.stream().map(FacetRequestColumnModel::getSearchConditionString).collect(Collectors.toList());

		return concatConditionStrings(conditionStrings);
	}


	/**
	 * Concatenates a list of query filter Strings with AND and then wraps that search condition with a parenthesis
	 * e.g. ["(col1 LIKE 'abc' OR col1 LIKE '%xyz')", "(col2 HAS('d'))"] => "( (col1 LIKE 'abc' OR col1 LIKE '%xyz') AND (col2 HAS('d')) )"
	 * @return the concatenated string or null if there was nothing to concatenate
	 */
	public static String concatQueryFilterConditionStrings(List<QueryFilter> filters){
		if(filters == null || filters.isEmpty()){
			return null;
		}

		return concatConditionStrings(filters.stream().map(FacetUtils::getSearchConditionString).collect(Collectors.toList()));
	}

	/**
	 * Concatenates a list of SQL conditional statements with " AND "
	 * @param conditionStrings
	 * @return
	 */
	private static String concatConditionStrings(List<String> conditionStrings) {
		ValidateArgument.required(conditionStrings, "conditionStrings");

		StringBuilder builder = new StringBuilder();
		builder.append("(");
		int initialSize = builder.length(); //length with the "( " included

		String joinedConditions = conditionStrings.stream()
				// Remove nulls
				.filter(s -> s != null && !s.isEmpty())
				// Join with " AND "
				.collect(Collectors.joining(" AND "));

		builder.append(joinedConditions);

		if(builder.length() == initialSize){ //edge case where nothing got concatenated together
			return null;
		}

		builder.append(")");
		return builder.toString();
	}

	/**
	 * Converts a QueryFilter to a Synapse SQL condition string
	 * @param filter
	 * @return a standalone condition, such as "( \"foo\" LIKE '%bar%' )"
	 */
	public static String getSearchConditionString(QueryFilter filter) {
		if (filter instanceof ColumnSingleValueQueryFilter) {
			ColumnSingleValueQueryFilter singleValueQueryFilter = (ColumnSingleValueQueryFilter) filter;
			return getCondition(singleValueQueryFilter);
		} else if (filter instanceof ColumnMultiValueFunctionQueryFilter) {
			ColumnMultiValueFunctionQueryFilter multiValueFnQueryFilter = (ColumnMultiValueFunctionQueryFilter) filter;
			return getCondition(multiValueFnQueryFilter);
		} else if (filter instanceof TextMatchesQueryFilter) {
			TextMatchesQueryFilter textMatchesQueryFilter = (TextMatchesQueryFilter) filter;
			return getCondition(textMatchesQueryFilter);
		} else {
			throw new IllegalArgumentException("Unknown filter type: " + filter.getClass().getName());
		}
	}

	private static String getCondition(ColumnSingleValueQueryFilter filter) {
		ValidateArgument.required(filter.getValues(), "values");
		ValidateArgument.required(filter.getColumnName(), "columnName");
		ValidateArgument.required(filter.getOperator(), "operator");
		String escapedColumn = "\"" + filter.getColumnName() + "\"";

		StringBuilder builder = new StringBuilder();
		builder.append("(");


		String conditions = filter.getValues().stream()
				// Build an individual condition for each value
				// e.g. "( \"col1\" LIKE '%value%' )"
				.map(value -> {
					String quotedValue = "'" + value + "'";
					return "(" +
							escapedColumn +
							" " +
							filter.getOperator().name() +
							" " +
							quotedValue +
							")";
				})
				// join conditions with " OR "
				.collect(Collectors.joining(" OR "));

		builder.append(conditions);
		builder.append(")");
		return builder.toString();
	}

	private static String getCondition(ColumnMultiValueFunctionQueryFilter filter) {
		ValidateArgument.required(filter.getValues(), "values");
		ValidateArgument.required(filter.getColumnName(), "columnName");
		ValidateArgument.required(filter.getFunction(), "function");

		String escapedColumn = "\"" + filter.getColumnName() + "\"";
		StringBuilder builder = new StringBuilder();

		builder.append(escapedColumn)
				.append(" ")
				.append(filter.getFunction().name())
				.append("(");

		String valuesAsFunctionParameters = filter.getValues().stream()
				// Wrap each value in single quotes
				.map(value -> "'"+value+"'")
				// Join with commas
				.collect(Collectors.joining(", "));
		builder.append(valuesAsFunctionParameters);
		builder.append(")");
		return builder.toString();
	}

	private static String getCondition(TextMatchesQueryFilter filter) {
		ValidateArgument.required(filter.getSearchExpression(), "searchExpression");
		StringBuilder builder = new StringBuilder();
		builder.append(TextMatchesPredicate.KEYWORD)
				.append("('")
				.append(filter.getSearchExpression())
				.append("')");
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
	public static QuerySpecification appendFacetSearchConditionToQuerySpecification(QuerySpecification sqlModel, List<FacetRequestColumnModel> facetRequestColumnModels) throws ParseException{
		QuerySpecification modelCopy = new TableQueryParser(sqlModel.toSql()).querySpecification();
		if(!facetRequestColumnModels.isEmpty()){
			WhereClause originalWhereClause = sqlModel.getTableExpression().getWhereClause();
			
			String facetSearchConditionString = FacetUtils.concatFacetSearchConditionStrings(facetRequestColumnModels, Optional.empty());
			
			StringBuilder builder = new StringBuilder();
			SqlElementUtils.appendCombinedWhereClauseToStringBuilder(builder, facetSearchConditionString, originalWhereClause);
			
			// create the new where if necessary
			if(builder.length() > 0){
				WhereClause newWhereClause = new TableQueryParser(builder.toString()).whereClause();
				modelCopy.getTableExpression().replaceWhere(newWhereClause);
			}
		}
		return modelCopy;
	}

	public static QuerySpecification appendQueryFiltersToQuerySpecification(QuerySpecification sqlModel, List<QueryFilter> filters) throws ParseException{
		QuerySpecification modelCopy = new TableQueryParser(sqlModel.toSql()).querySpecification();
		if(filters != null && !filters.isEmpty()){
			WhereClause originalWhereClause = sqlModel.getTableExpression().getWhereClause();

			String facetSearchConditionString = FacetUtils.concatQueryFilterConditionStrings(filters);

			StringBuilder builder = new StringBuilder();
			SqlElementUtils.appendCombinedWhereClauseToStringBuilder(builder, facetSearchConditionString, originalWhereClause);

			// create the new where if necessary
			if(builder.length() > 0){
				WhereClause newWhereClause = new TableQueryParser(builder.toString()).whereClause();
				modelCopy.getTableExpression().replaceWhere(newWhereClause);
			}
		}
		return modelCopy;
	}
}
