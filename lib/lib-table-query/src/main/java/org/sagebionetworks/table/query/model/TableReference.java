package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * This matches &lttable reference&gt in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
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
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(tableName);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		// this element does not contain any SQLElements
	}
	
	/**
	 * Replace the table name.
	 * @param tableName
	 */
	public void replaceTableName(String tableName){
		this.tableName = tableName;
	}
}
