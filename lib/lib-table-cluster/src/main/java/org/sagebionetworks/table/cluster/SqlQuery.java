package org.sagebionetworks.table.cluster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.util.SqlElementUntils;

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
	 * This map will contain all of the bind variable values for the translated query.
	 */
	Map<String, Object> parameters;
	
	/**
	 * The map of column names to column IDs.
	 */
	Map<String, ColumnModel> columnNameToModelMap;
	
	/**
	 * The translated SQL.
	 */
	String outputSQL;
	
	/**
	 * The Id of the table.
	 */
	String tableId;
	
	/**
	 * Aggregated results are queries that included one or more aggregation functions in the select clause.
	 * These query results will not match columns in the table. In addition rowIDs and rowVersionNumbers
	 * will be null when isAggregatedResults = true.
	 */
	boolean isAggregatedResult;
	
	/**
	 * The list of all column ID referenced in the select column.
	 */
	List<Long> selectColumnIds;
	
	
	/**
	 * Create a new SQLQuery from an input SQL string and mapping of the column names to column IDs.
	 * 
	 * @param sql
	 * @param columnNameToModelMap
	 * @throws ParseException
	 */
	public SqlQuery(String sql, Map<String, ColumnModel> columnNameToModelMap) throws ParseException {
		if(sql == null) throw new IllegalArgumentException("The input SQL cannot be null");
		init(TableQueryParser.parserQuery(sql), columnNameToModelMap);
	}
	
	/**
	 * Create a query with a parsed model.
	 * 
	 * @param model
	 * @param columnNameToModelMap
	 * @throws ParseException
	 */
	public SqlQuery(QuerySpecification model, Map<String, ColumnModel> columnNameToModelMap) {
		if(model == null) throw new IllegalArgumentException("The input model cannot be null");
		init(model, columnNameToModelMap);
	}

	/**
	 * @param sql
	 * @param columnNameToModelMap
	 * @throws ParseException
	 */
	public void init(QuerySpecification model, Map<String, ColumnModel> columnNameToModelMap) {
		if (columnNameToModelMap == null)
			throw new IllegalArgumentException("columnNameToModelMap cannot be null");
		this.model = model;
		this.tableId = SqlElementUntils.getTableId(model);
		// This string builder is used to build up the output SQL.
		StringBuilder outputBuilder = new StringBuilder();
		// This map will contain all of the 
		this.parameters = new HashMap<String, Object>();	
		this.columnNameToModelMap = columnNameToModelMap;
		isAggregatedResult = SQLTranslatorUtils.translate(this.model, outputBuilder, this.parameters, this.columnNameToModelMap);
		this.outputSQL = outputBuilder.toString();
		this.selectColumnIds = SQLTranslatorUtils.getSelectColumns(this.model.getSelectList(), columnNameToModelMap);
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
	public Map<String, ColumnModel> getcolumnNameToModelMap() {
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
	 * The list of column IDs from the select clause.
	 * @return
	 */
	public List<Long> getSelectColumnIds() {
		return selectColumnIds;
	}
	
	
}
