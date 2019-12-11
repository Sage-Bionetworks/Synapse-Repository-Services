package org.sagebionetworks.table.query.model;

/**
 * This matches &lttable reference&gt in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class TableReference extends SimpleBranch {

	public TableReference(TableName tableName) {
		super(tableName);
	}

	//not used by parser.
	//also skipped <joined table> part of the spec and heads directly to <qualified join>
	public TableReference(QualifiedJoin joinedTable) {super(joinedTable);}

	public String getTableName() {
		return child.toSql();
	}
}
