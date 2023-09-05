package org.sagebionetworks.table.query.model;

import java.util.Collections;

/**
 * Used internally to define a json column within a JSON_TABLE expression for unnesting multi-value columns
 */
public class JsonTableColumn extends SQLElement {
	
	private String columnName;
	private String mySqlType;
	private String jsonPath;

	public JsonTableColumn(String columnName, String mySqlType) {
		this.columnName = columnName;
		this.mySqlType = mySqlType;
		this.jsonPath = "'$'";
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(columnName).append(" ").append(mySqlType).append(" PATH ").append(jsonPath).append(" ERROR ON ERROR");
	}

	@Override
	public Iterable<Element> getChildren() {
		return Collections.emptyList();
	}

}
