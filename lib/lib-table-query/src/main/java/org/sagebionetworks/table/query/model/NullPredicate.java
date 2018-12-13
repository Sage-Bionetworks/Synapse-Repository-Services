package org.sagebionetworks.table.query.model;

/**
 * This matches &ltnull predicate&gt  in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class NullPredicate extends IsPredicate  {
	

	public NullPredicate(ColumnReference columnReferenceLHS, Boolean not) {
		super(columnReferenceLHS, not);
	}

	@Override
	public String getCompareValue() {
		return "NULL";
	}	
}
