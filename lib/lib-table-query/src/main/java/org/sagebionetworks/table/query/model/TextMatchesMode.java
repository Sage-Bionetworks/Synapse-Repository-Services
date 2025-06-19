package org.sagebionetworks.table.query.model;

public enum TextMatchesMode {
	NATURAL_LANGUAGE("IN NATURAL LANGUAGE MODE"),
	BOOLEAN("IN BOOLEAN MODE");

	private String sql;
	
	TextMatchesMode(String sql) {
		this.sql = sql;
	}
	
	public String getSql() {
		return sql;
	}
}
