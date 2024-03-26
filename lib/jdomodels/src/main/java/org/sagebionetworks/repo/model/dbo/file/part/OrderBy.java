package org.sagebionetworks.repo.model.dbo.file.part;

public enum OrderBy {
	random("RAND()"), asc("L.PART_RANGE_LOWER_BOUND ASC");

	private final String sql;

	OrderBy(String sql) {
		this.sql = sql;
	}

	public String toSql() {
		return sql;
	}
}
