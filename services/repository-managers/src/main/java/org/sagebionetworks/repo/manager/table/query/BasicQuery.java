package org.sagebionetworks.repo.manager.table.query;

import java.util.Map;
import java.util.Objects;

public class BasicQuery {
	
	private final String sql;
	private final Map<String, Object> parameters;
	
	public BasicQuery(String sql, Map<String, Object> parameters) {
		super();
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
	

}
