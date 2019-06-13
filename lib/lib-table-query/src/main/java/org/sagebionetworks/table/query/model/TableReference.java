package org.sagebionetworks.table.query.model;

/**
 * This matches &lttable reference&gt in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class TableReference extends SimpleBranch {

	public TableReference(TableName tableName) {
		super(tableName);
	}

	public String getTableName() {
		return child.toSql();
	}
	
	/**
	 * Replace the table name.
	 * @param tableName
	 */
	public void replaceTableName(String tableName){
		this.child = new TableName(new RegularIdentifier(tableName));
	}
}
