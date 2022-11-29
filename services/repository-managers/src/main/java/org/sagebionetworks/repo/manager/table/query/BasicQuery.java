package org.sagebionetworks.repo.manager.table.query;

import java.util.Map;
import java.util.Objects;

import org.sagebionetworks.util.ValidateArgument;

/**
 * A basic query composed of both SQL to run and the query parameters.
 * Note: The SQL must already be translated.
 *
 */
public class BasicQuery {
	
	private final String sql;
	private final Map<String, Object> parameters;
	
	public BasicQuery(String sql, Map<String, Object> parameters) {
		ValidateArgument.required(sql, "sql");
		ValidateArgument.required(parameters, "parameters");
		this.sql = sql;
		this.parameters = parameters;
	}

	/**
	 * @return the sql
	 */
	public String getSql() {
		return sql;
	}

	/**
	 * @return the parameters
	 */
	public Map<String, Object> getParameters() {
		return parameters;
	}

	@Override
	public int hashCode() {
		return Objects.hash(parameters, sql);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof BasicQuery)) {
			return false;
		}
		BasicQuery other = (BasicQuery) obj;
		return Objects.equals(parameters, other.parameters) && Objects.equals(sql, other.sql);
	}

	@Override
	public String toString() {
		return "BasicQuery [sql=" + sql + ", parameters=" + parameters + "]";
	}
	

}
