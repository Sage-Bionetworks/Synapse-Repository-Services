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
	 * Translate the input SQL.
	 * @param sql
	 * @throws ParseException 
	 */
	public SqlTranslator(String sql) throws ParseException{
		if(sql == null) throw new IllegalArgumentException("The input SQL cannot be null");
		// Parse the SQL
		model = TableQueryParser.parserQuery(sql);
		// This string builder is used to build up the output SQL.
		StringBuilder outputBuilder = new StringBuilder();
		// This map will contain all of the 
		parameters = new HashMap<String, Object>();	
		
		SQLTranslatorUtils.translate(model, outputBuilder, parameters);
	}
}
