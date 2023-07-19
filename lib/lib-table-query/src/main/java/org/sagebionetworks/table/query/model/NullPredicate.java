package org.sagebionetworks.table.query.model;

/**
 * This matches &ltnull predicate&gt  in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class NullPredicate extends IsPredicate  {
	

	public NullPredicate(PredicateLeftHandSide leftHandSide, Boolean not) {
		super(leftHandSide, not);
	}

	@Override
	public String getCompareValue() {
		return "NULL";
	}	
}
