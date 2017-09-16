package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 *  GroupingColumnReference ::= {@link ColumnReference} [ <collate clause> ]
 */
public class GroupingColumnReference extends SQLElement {

	ColumnReference columnReference;

	public GroupingColumnReference(ColumnReference columnReference) {
		this.columnReference = columnReference;
	}

	public ColumnReference getColumnReference() {
		return columnReference;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		columnReference.toSql(builder, parameters);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, columnReference);
	}
}
