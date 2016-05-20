package org.sagebionetworks.table.query.model;

import java.util.List;


/**
 * This matches &ltfrom clause&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class FromClause extends SQLElement {

	private TableReference tableReference;

	public FromClause(TableReference tableReference) {
		super();
		this.tableReference = tableReference;
	}

	public TableReference getTableReference() {
		return tableReference;
	}

	@Override
	public void toSql(StringBuilder builder) {
		builder.append("FROM ");
		tableReference.toSql(builder);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, tableReference);
	}
}
