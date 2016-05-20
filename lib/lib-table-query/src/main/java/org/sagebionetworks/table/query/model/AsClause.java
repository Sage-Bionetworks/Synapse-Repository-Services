package org.sagebionetworks.table.query.model;

import java.util.List;


/**
 * This matches &ltas clause&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class AsClause extends SQLElement {
	
	ColumnName columnName;

	public AsClause(ColumnName columnName) {
		this.columnName = columnName;
	}

	public ColumnName getColumnName() {
		return columnName;
	}

	@Override
	public void toSql(StringBuilder builder) {
		builder.append("AS ");
		columnName.toSql(builder);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, columnName);
	}
	
}
