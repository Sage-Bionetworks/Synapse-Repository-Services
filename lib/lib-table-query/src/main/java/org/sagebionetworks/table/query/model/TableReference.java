package org.sagebionetworks.table.query.model;

/**
 * This matches &lttable reference&gt in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class TableReference extends SimpleBranch {

	public TableReference(TableNameCorrelation tableName) {
		super(tableName);
	}
	
	public TableReference(QualifiedJoin qualifiedJoin) {
		super(qualifiedJoin);
	}

	public String getTableName() {
		if(child instanceof TableNameCorrelation) {
			return child.toSql();
		}else {
			throw new IllegalArgumentException("JOIN not supported in this context");
		}
	}
	
	/**
	 * Does this table reference have one or more joins?
	 * @return
	 */
	public boolean hasJoin() {
		return child.getFirstElementOfType(QualifiedJoin.class) != null;
	}
}
