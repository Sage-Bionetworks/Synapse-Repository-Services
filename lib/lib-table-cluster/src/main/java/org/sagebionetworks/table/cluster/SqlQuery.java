package org.sagebionetworks.table.cluster;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;

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
	Map<String, Long> columnNameToIdMap;
	
	/**
	 * The original input SQL.
	 */
	String inputSQL;
	/**
	 * The translated SQL.
	 */
	String outputSQL;
	
	/**
	 * Aggregated results are queries that included one or more aggregation functions in the select clause.
	 * These query results will not match columns in the table. In addition rowIDs and rowVersionNumbers
	 * will be null when isAggregatedResults = true.
	 */
	boolean isAggregatedResult;
	

	
	/**
	 * Create a new SQLQuery from an input SQL string and mapping of the column names to column IDs.
	 * 
	 * @param sql
	 * @param columnNameToIdMap
	 * @throws ParseException
	 */
	public SqlQuery(String sql, Map<String, Long> columnNameToIdMap) throws ParseException{
		if(sql == null) throw new IllegalArgumentException("The input SQL cannot be null");
		if(columnNameToIdMap == null) throw new IllegalArgumentException("columnNameToIdMap cannot be null");
		this.inputSQL = sql;
		// Parse the SQL
		this.model = TableQueryParser.parserQuery(sql);
		// This string builder is used to build up the output SQL.
		StringBuilder outputBuilder = new StringBuilder();
		// This map will contain all of the 
		this.parameters = new HashMap<String, Object>();	
		this.columnNameToIdMap = columnNameToIdMap;
		isAggregatedResult = SQLTranslatorUtils.translate(this.model, outputBuilder, this.parameters, this.columnNameToIdMap);
		this.outputSQL = outputBuilder.toString();
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
	public Map<String, Long> getColumnNameToIdMap() {
		return columnNameToIdMap;
	}


	/**
	 * The original input SQL
	 * 
	 * @return
	 */
	public String getInputSQL() {
		return inputSQL;
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
	
	
}
