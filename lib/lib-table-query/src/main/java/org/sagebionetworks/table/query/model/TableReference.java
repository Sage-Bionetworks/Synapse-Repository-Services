package org.sagebionetworks.table.query.model;

/**
 * This matches &lttable reference&gt in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class TableReference extends SimpleBranch {

	public TableReference(TableName tableName) {
		super(tableName);
	}

	//not exposed to parser. currently only allows a single join since that's all that is necessary
	//also skipped <joined table> part of the spec and heads directly to <qualified join>
	public TableReference(QualifiedJoin joinedTable) {super(joinedTable);}

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

	public void replaceTableName(QualifiedJoin qualifiedJoin){
		this.replaceChildren(qualifiedJoin);
	}
}
