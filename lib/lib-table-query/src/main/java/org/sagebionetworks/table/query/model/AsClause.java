package org.sagebionetworks.table.query.model;


/**
 * This matches &ltas clause&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class AsClause extends SQLElement {
	
	ColumnName columnName;

	public AsClause(ColumnName columnName) {
		super();
		this.columnName = columnName;
	}

	public ColumnName getColumnName() {
		return columnName;
	}

	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		builder.append("AS ");
		this.columnName.toSQL(builder, columnConvertor);
		if (columnConvertor != null) {
			columnConvertor.addAsColumn(columnName);
		}
	}
	
}
