package org.sagebionetworks.table.query.model;

/**
 * This matches &lttable reference&gt in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class TableReference extends SimpleBranch {

	public TableReference(TableName tableName) {
		super(tableName);
	}
	
	public TableReference(QualifiedJoin qualifiedJoin) {
		super(qualifiedJoin);
	}

	public String getTableName() {
		return child.toSql();
	}
}
