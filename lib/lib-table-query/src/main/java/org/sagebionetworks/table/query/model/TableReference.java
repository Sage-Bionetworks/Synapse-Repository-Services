package org.sagebionetworks.table.query.model;

import java.util.Optional;

/**
 * This matches &lttable reference&gt in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class TableReference extends SimpleBranch implements HasSingleTableName, Replaceable<TableReference> {

	public TableReference(TableNameCorrelation tableName) {
		super(tableName);
	}
	
	public TableReference(QualifiedJoin qualifiedJoin) {
		super(qualifiedJoin);
	}
	
	/**
	 * Does this table reference have one or more joins?
	 * @return
	 */
	public boolean hasJoin() {
		return child.getFirstElementOfType(QualifiedJoin.class) != null;
	}

	@Override
	public Optional<String> getSingleTableName() {
		if(!(child instanceof HasSingleTableName)) {
			return Optional.empty();
		}
		return ((HasSingleTableName)child).getSingleTableName();
	}
}
