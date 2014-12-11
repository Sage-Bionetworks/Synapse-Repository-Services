package org.sagebionetworks.table.query.model;

/**
 * This matches &lttable reference&gt in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class TableReference extends SQLElement {

	String tableName;

	public TableReference(String tableName) {
		this.tableName = tableName;
	}

	public String getTableName() {
		return tableName;
	}

	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		if (columnConvertor != null) {
			columnConvertor.convertTableName(tableName, builder);
		} else {
			builder.append(tableName);
		}
	}

}
