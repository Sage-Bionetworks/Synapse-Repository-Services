package org.sagebionetworks.table.query.model;

import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

/**
 * Used internally to represent a JSON_TABLE expression used to unnest multi-value columns
 */
public class JsonTable extends SQLElement {
	
	private ColumnReference columnReference;
	private List<JsonTableColumn> columns;
	private CorrelationSpecification correlationSpecification;

	public JsonTable(ColumnReference columnReference, List<JsonTableColumn> columns, CorrelationSpecification correlationSpecification) {
		this.columnReference = columnReference;
		this.columns = columns;
		this.correlationSpecification = correlationSpecification;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("JSON_TABLE(");
		columnReference.toSql(builder, parameters);
		builder.append(", '$[*]' COLUMNS(");
		boolean firstColumn = true;
		for (JsonTableColumn column : columns) {
			if (!firstColumn) {
				builder.append(",");
			}
			column.toSql(builder, parameters);
			firstColumn = false;
		}
		builder.append("))");
		if (correlationSpecification != null) {
			builder.append(" ");
			correlationSpecification.toSql(builder, parameters);
		}
	}

	@Override
	public Iterable<Element> getChildren() {
		return Collections.emptyList();
	}

}
