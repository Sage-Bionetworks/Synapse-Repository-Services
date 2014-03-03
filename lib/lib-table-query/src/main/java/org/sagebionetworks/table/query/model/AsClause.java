package org.sagebionetworks.table.query.model;

/**
 * This matches &ltas clause&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class AsClause implements SQLElement {
	
	ColumnName columnName;

	public AsClause(ColumnName columnName) {
		super();
		this.columnName = columnName;
	}

	public ColumnName getColumnName() {
		return columnName;
	}

	@Override
	public void toSQL(StringBuilder builder) {
		builder.append("AS ");
		this.columnName.toSQL(builder);
	}
	
}
