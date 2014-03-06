package org.sagebionetworks.table.cluster;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;

/**
 * Translates from the user input SQL into a form that can be run against the database cluster.
 * @author John
 *
 */
public class SqlTranslator {
	
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
	
	String inputSQL;
	String outputSQL;
	
	
	/**
	 * Translate the input SQL.
	 * @param sql
	 * @throws ParseException 
	 */
	public SqlTranslator(String sql, Map<String, Long> columnNameToIdMap) throws ParseException{
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
		SQLTranslatorUtils.translate(this.model, outputBuilder, this.parameters, this.columnNameToIdMap);
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
	 * 
	 * @return
	 */
	public Map<String, Long> getColumnNameToIdMap() {
		return columnNameToIdMap;
	}


	/**
	 * The input SQL that was translated to the output SQL.
	 * 
	 * @return
	 */
	public String getInputSQL() {
		return inputSQL;
	}

	/**
	 * The translated oupt SQL.
	 * @return
	 */
	public String getOutputSQL() {
		return outputSQL;
	}
	
	
}
