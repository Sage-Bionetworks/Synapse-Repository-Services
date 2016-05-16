package org.sagebionetworks.table.cluster;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Represents a SQL query for a table.
 * 
 * @author John
 *
 */
public class SqlQuery {
	
	/**
	 * The input SQL is parsed into this object model.
	 * 
	 */
	QuerySpecification model;
	
	/**
	 * The full list of all of the columns of this table
	 */
	List<ColumnModel> tableSchema;
	
	/**
	 * This map will contain all of the bind variable values for the translated query.
	 */
	Map<String, Object> parameters;
	
	/**
	 * The map of column names to column models.
	 */
	LinkedHashMap<String, ColumnModel> columnNameToModelMap;
	
	/**
	 * The translated SQL.
	 */
	String outputSQL;
	
	/**
	 * The Id of the table.
	 */
	String tableId;
	
	/**
	 * Does this query include ROW_ID and ROW_VERSION?
	 */
	boolean includesRowIdAndVersion;
	
	/**
	 * Aggregated results are queries that included one or more aggregation functions in the select clause.
	 * These query results will not match columns in the table. In addition rowIDs and rowVersionNumbers
	 * will be null when isAggregatedResults = true.
	 */
	boolean isAggregatedResult;
	
	/**
	 * The list of all columns referenced in the select column.
	 */
	List<SelectColumn> selectColumns;
	
	
	/**
	 * Create a new SQLQuery from an input SQL string and mapping of the column names to column IDs.
	 * 
	 * @param sql
	 * @param columnNameToModelMap
	 * @throws ParseException
	 */
	public SqlQuery(String sql, List<ColumnModel> tableSchema) throws ParseException {
		if(sql == null) throw new IllegalArgumentException("The input SQL cannot be null");
		QuerySpecification parsedQuery = TableQueryParser.parserQuery(sql);
		init(parsedQuery, tableSchema, parsedQuery.getTableName());
	}
	
	/**
	 * Create a query with a parsed model.
	 * 
	 * @param model
	 * @param columnNameToModelMap
	 * @throws ParseException
	 */
	public SqlQuery(QuerySpecification model, List<ColumnModel> tableSchema, String tableId) {
		if (model == null)
			throw new IllegalArgumentException("The input model cannot be null");
		init(model, tableSchema, tableId);
	}

	/**
	 * @param tableId
	 * @param sql
	 * @param columnNameToModelMap
	 * @throws ParseException
	 */
	public void init(QuerySpecification parsedModel, List<ColumnModel> tableSchema, String tableId) {
		ValidateArgument.required(tableSchema, "TableSchema");
		ValidateArgument.required(tableId, "tableId");
		if(tableSchema.isEmpty()){
			throw new IllegalArgumentException("Table schema cannot be empty");
		}
		this.tableSchema = tableSchema;
		this.model = parsedModel;
		this.tableId = tableId;

		// This map will contain all of the 
		this.parameters = new HashMap<String, Object>();	
		this.columnNameToModelMap = TableModelUtils.createColumnNameToModelMap(tableSchema);
		// SELECT * is replaced with a select including each column in the schema.
		if (BooleanUtils.isTrue(this.model.getSelectList().getAsterisk())) {
			SelectList expandedSelectList = SQLTranslatorUtils.createSelectListFromSchema(tableSchema);
			this.model.replaceSelectList(expandedSelectList);
		}
		// Track if this is an aggregate query.
		this.isAggregatedResult = model.hasAnyAggregateElements();
		// Build headers that describe how the client should read the results of this query.
		this.selectColumns = SQLTranslatorUtils.getSelectColumns(this.model.getSelectList(), columnNameToModelMap, this.isAggregatedResult);

		// Create a copy of the original model.
		QuerySpecification transformedModel = model;
		// Add ROW_ID and ROW_VERSION only if all columns have an Id.
		if (SQLTranslatorUtils.doAllSelectMatchSchema(selectColumns)) {
			// we need to add the row count and row version columns
			SelectList expandedSelectList = SQLTranslatorUtils.addRowIdAndVersionToSelect(this.model.getSelectList());
			transformedModel = new QuerySpecification(model.getSqlDirective(), model.getSetQuantifier(), expandedSelectList, model.getTableExpression());
			this.includesRowIdAndVersion = true;
		}else{
			this.includesRowIdAndVersion = false;
		}

		this.outputSQL = SQLTranslatorUtils.translate(transformedModel, this.parameters, this.columnNameToModelMap);

	}
	
	/**
	 * Does this query include ROW_ID and ROW_VERSION
	 * 
	 * @return
	 */
	public boolean includesRowIdAndVersion(){
		return this.includesRowIdAndVersion;
	}

	/**
	 * The input SQL was parsed into this model object.
	 * 
	 * @return
	 */
	public QuerySpecification getModel() {
		return model;
	}


	/**
	 * This map contains the values of all bind variables referenced in the translated output SQL.
	 * @return
	 */
	public Map<String, Object> getParameters() {
		return parameters;
	}


	/**
	 * The column name to column ID mapping.
	 * @return
	 */
	public Map<String, ColumnModel> getColumnNameToModelMap() {
		return columnNameToModelMap;
	}


	/**
	 * The translated output SQL.
	 * @return
	 */
	public String getOutputSQL() {
		return outputSQL;
	}

	/**
	 * Aggregated results are queries that included one or more aggregation functions in the select clause.
	 * These query results will not match columns in the table. In addition rowIDs and rowVersionNumbers
	 * will be null when isAggregatedResults = true.
	 * @return
	 */
	public boolean isAggregatedResult() {
		return isAggregatedResult;
	}

	/**
	 * The ID of the table.
	 * @return
	 */
	public String getTableId() {
		return tableId;
	}

	/**
	 * The list of column models from the select clause.
	 * @return
	 */
	public List<SelectColumn> getSelectColumns() {
		return selectColumns;
	}

	/**
	 * All of the Columns of the table.
	 * @return
	 */
	public List<ColumnModel> getTableSchema() {
		return tableSchema;
	}
}
