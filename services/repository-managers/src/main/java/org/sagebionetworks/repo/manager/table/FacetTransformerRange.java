package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.repo.model.table.FacetColumnResult;
import org.sagebionetworks.repo.model.table.FacetColumnResultRange;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.model.TableExpression;
import org.sagebionetworks.util.ValidateArgument;

public class FacetTransformerRange implements FacetTransformer {
	public static final String MIN_ALIAS = "minimum";
	public static final String MAX_ALIAS = "maximum";
	
	private String columnName;
	private List<ValidatedQueryFacetColumn> facets;
	private SqlQuery originalQuery;
	
	private SqlQuery generatedFacetSqlQuery = null;
	
	public FacetTransformerRange(String columnName, List<ValidatedQueryFacetColumn> facets, SqlQuery originalQuery){
		ValidateArgument.required(columnName, "columnName");
		ValidateArgument.required(facets, "facets");
		ValidateArgument.required(originalQuery, "originalQuery");
		this.columnName = columnName;
		this.facets = facets;
		this.originalQuery = originalQuery;
	}
	
	@Override
	public String getColumnName() {
		return this.columnName;
	}

	
	/**
	 * Creates a new SQL query for finding the minimum and maximum values of a faceted column
	 * @param columnName the name of the faceted column
	 * @param model model the original (non-transformed) query off of which to obtain the FROM and WHERE clauses
	 * @param facetSearchConditionString
	 * @return the generated SQL query represented by QuerySpecification
	 */
	@Override
	public SqlQuery getFacetSqlQuery() {
		if(this.generatedFacetSqlQuery != null) return this.generatedFacetSqlQuery;
		
		String facetSearchConditionString = FacetUtils.concatFacetSearchConditionStrings(columnName, facets);
		
		TableExpression tableExpressionFromModel = originalQuery.getModel().getTableExpression();
		StringBuilder builder = new StringBuilder("SELECT MIN(");
		builder.append(columnName);
		builder.append(") as ");
		builder.append(MIN_ALIAS);
		builder.append(", MAX(");
		builder.append(columnName);
		builder.append(") as ");
		builder.append(MAX_ALIAS);
		builder.append(" ");
		builder.append(tableExpressionFromModel.getFromClause().toSql());
		FacetUtils.appendFacetWhereClauseToStringBuilderIfNecessary(builder, facetSearchConditionString, tableExpressionFromModel.getWhereClause());
		
		try {
			this.generatedFacetSqlQuery =  new SqlQuery(builder.toString(), originalQuery.getTableSchema());
			return this.generatedFacetSqlQuery;
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		
	}

	@Override
	public FacetColumnResult translateToResult(RowSet rowSet) {
		List<SelectColumn> headers = rowSet.getHeaders();
		//check for expected headers
		if(headers.size() != 2 || !headers.get(0).equals(MIN_ALIAS) || !headers.get(1).equals(MAX_ALIAS)){
			throw new IllegalArgumentException("The RowSet's headers did not contain the expected column names");
		}
		
		List<Row> rows = rowSet.getRows();
		//should only have one row
		if(rows.size() != 1){ 
			throw new IllegalArgumentException("Expected RowSet to only have one row");
		}
		
		Row row = rows.get(0);
		
		FacetColumnResultRange result = new FacetColumnResultRange();
		result.setColumnName(this.columnName);
		result.setFacetType(FacetType.range);
		List<String> values =  row.getValues();
		result.setColumnMin(values.get(0));
		result.setColumnMax(values.get(1));
		
		
		
		return result;
	}

}
