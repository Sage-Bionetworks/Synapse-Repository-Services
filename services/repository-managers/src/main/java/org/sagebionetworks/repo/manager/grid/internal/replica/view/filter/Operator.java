package org.sagebionetworks.repo.manager.grid.internal.replica.view.filter;

/**
 * Basic SQL operators as an enumeration.
 */
public enum Operator {

	eq("="), gt(">"), lt("<"), gte(">="), lte("<="), ne("<>");

	private String sql;

	private Operator(String sql) {
		this.sql = sql;
	}

	public String getSql() {
		return sql;
	}
}
