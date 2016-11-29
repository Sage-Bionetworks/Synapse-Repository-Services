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
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.model.Pagination;
import org.sagebionetworks.table.query.model.TableExpression;
import org.sagebionetworks.util.ValidateArgument;

public class FacetTransformerValueCounts implements FacetTransformer {
	public static final String VALUE_ALIAS = "value";
	public static final String COUNT_ALIAS = "frequency";
	public static final long MAX_NUM_FACET_CATEGORIES = 100;
	
	
	private String columnName;
	private List<ValidatedQueryFacetColumn> facets;
	
	private SqlQuery generatedFacetSqlQuery;
	private Set<String> selectedValues;
	
	public FacetTransformerValueCounts(String columnName, List<ValidatedQueryFacetColumn> facets, SqlQuery originalQuery, Set<String> selectedValues){
		ValidateArgument.required(columnName, "columnName");
		ValidateArgument.required(facets, "facets");
		ValidateArgument.required(originalQuery, "originalQuery");
		this.columnName = columnName;
		this.facets = facets;
		this.selectedValues = selectedValues;
		this.generatedFacetSqlQuery = generateFacetSqlQuery(originalQuery);
	}
	
	
	@Override
	public String getColumnName() {
		return this.columnName;
	}

	@Override
	public SqlQuery getFacetSqlQuery(){
		return this.generatedFacetSqlQuery;
	}
	
	private SqlQuery generateFacetSqlQuery(SqlQuery originalQuery) {
		String facetSearchConditionString = FacetUtils.concatFacetSearchConditionStrings(facets, columnName);
		
		TableExpression tableExpressionFromModel = originalQuery.getModel().getTableExpression();
		Pagination pagination = new Pagination(MAX_NUM_FACET_CATEGORIES, null);
		StringBuilder builder = new StringBuilder("SELECT ");
		builder.append(columnName);
		builder.append(" AS ");
		builder.append(VALUE_ALIAS);
		builder.append(", COUNT(*) AS ");
		builder.append(COUNT_ALIAS);
		builder.append(" ");
		builder.append(tableExpressionFromModel.getFromClause().toSql());
		FacetUtils.appendFacetWhereClauseToStringBuilderIfNecessary(builder, facetSearchConditionString, tableExpressionFromModel.getWhereClause());
		builder.append(" GROUP BY " + columnName + " ");
		builder.append(pagination.toSql());
		
		try {
			return new SqlQuery(builder.toString(), originalQuery.getTableSchema());
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
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
		result.setFacetType(FacetType.enumeration);
		result.setFacetValues(valueCounts);
		
		return result;
	}

}
