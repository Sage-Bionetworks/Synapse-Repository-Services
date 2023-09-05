package org.sagebionetworks.repo.manager.table;

import static org.sagebionetworks.repo.model.table.TableConstants.NULL_VALUE_KEYWORD;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.table.FacetColumnResult;
import org.sagebionetworks.repo.model.table.FacetColumnResultValueCount;
import org.sagebionetworks.repo.model.table.FacetColumnResultValues;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.table.cluster.QueryTranslator;
import org.sagebionetworks.table.cluster.TranslationDependencies;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.FromClause;
import org.sagebionetworks.table.query.model.NonJoinQueryExpression;
import org.sagebionetworks.table.query.model.Pagination;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.model.WhereClause;
import org.sagebionetworks.table.query.util.FacetRequestColumnModel;
import org.sagebionetworks.table.query.util.FacetUtils;
import org.sagebionetworks.table.query.util.SqlElementUtils;
import org.sagebionetworks.util.ValidateArgument;

public class FacetTransformerValueCounts implements FacetTransformer {
	public static final String VALUE_ALIAS = "value";
	public static final String COUNT_ALIAS = "frequency";
	public static final long MAX_NUM_FACET_CATEGORIES = 100;
	
	
	private String columnName;
	private String jsonPath;
	private List<FacetRequestColumnModel> facets;
	
	private QueryTranslator generatedFacetSqlQuery;
	private Set<String> selectedValues;
	
	public FacetTransformerValueCounts(String columnName, String jsonPath, boolean columnTypeIsList, List<FacetRequestColumnModel> facets,
			QueryExpression originalQuery, TranslationDependencies dependencies, Set<String> selectedValues){
		ValidateArgument.required(columnName, "columnName");
		ValidateArgument.required(facets, "facets");
		ValidateArgument.required(originalQuery, "originalQuery");
		this.columnName = columnName;
		this.jsonPath = jsonPath;
		this.facets = facets;
		this.selectedValues = selectedValues;
		this.generatedFacetSqlQuery = generateFacetSqlQuery(originalQuery, dependencies, columnTypeIsList);
	}
	
	
	@Override
	public String getColumnName() {
		return this.columnName;
	}

	@Override
	public QueryTranslator getFacetSqlQuery(){
		return this.generatedFacetSqlQuery;
	}
	
	private QueryTranslator generateFacetSqlQuery(QueryExpression originalQuery, TranslationDependencies dependencies, boolean columnTypeIsList) {
		String columnNameExpression = FacetUtils.getColumnNameExpression(columnName, jsonPath);
		
		String facetSearchConditionString = FacetUtils.concatFacetSearchConditionStrings(facets, columnNameExpression);
		
		Pagination pagination = new Pagination(MAX_NUM_FACET_CATEGORIES, null);

		String columnToUse = columnTypeIsList ? "UNNEST(" + columnNameExpression + ")" : columnNameExpression;
		
		NonJoinQueryExpression njqe = originalQuery.getNonJoinQueryExpression();

		StringBuilder builder = new StringBuilder("SELECT ");
		builder.append(columnToUse);
		builder.append(" AS ");
		builder.append(VALUE_ALIAS);
		builder.append(", COUNT(*) AS ");
		builder.append(COUNT_ALIAS);
		builder.append(" ");
		builder.append(njqe.getFirstElementOfType(FromClause.class).toSql());
		SqlElementUtils.appendCombinedWhereClauseToStringBuilder(builder, facetSearchConditionString, njqe.getFirstElementOfType(WhereClause.class));
		builder.append(" GROUP BY ");
		builder.append(columnToUse);
		builder.append(" ORDER BY ");
		builder.append(COUNT_ALIAS);
		builder.append(" DESC, ");
		builder.append(VALUE_ALIAS);
		builder.append(" ASC ");
		builder.append(pagination.toSql());
		
		try {
			NonJoinQueryExpression replacement = new TableQueryParser(builder.toString()).nonJoinQueryExpression();
			njqe.replaceElement(replacement);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		
		return QueryTranslator.builder(originalQuery.toSql(), dependencies).build();
	}

	@Override
	public FacetColumnResult translateToResult(RowSet rowSet) {
		ValidateArgument.required(rowSet, "rowSet");
		
		List<SelectColumn> headers = rowSet.getHeaders();
		//check for expected headers
		if(headers.size() != 2 || !headers.get(0).getName().equals(VALUE_ALIAS) || !headers.get(1).getName().equals(COUNT_ALIAS)){
			throw new IllegalArgumentException("The RowSet's headers did not contain the expected column names" + headers);
		}
		
		List<FacetColumnResultValueCount> valueCounts = new ArrayList<>();
		for(Row row : rowSet.getRows()){
			List<String> rowValues  = row.getValues();
			String value = rowValues.get(0);
			if(value == null){
				//for counts of unset values
				value = NULL_VALUE_KEYWORD;
			}
			FacetColumnResultValueCount valCount = new FacetColumnResultValueCount();
			valCount.setValue(value);
			valCount.setIsSelected(selectedValues != null && selectedValues.contains(value));
			valCount.setCount(Long.parseLong(rowValues.get(1)));
			valueCounts.add(valCount);
		}
		
		FacetColumnResultValues result = new FacetColumnResultValues();
		result.setColumnName(this.columnName);
		result.setJsonPath(this.jsonPath);
		result.setFacetType(FacetType.enumeration);
		result.setFacetValues(valueCounts);
		
		return result;
	}

}
