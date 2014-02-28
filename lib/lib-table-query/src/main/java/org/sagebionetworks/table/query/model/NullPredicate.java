package org.sagebionetworks.table.query.model;

/**
 * This matches &ltnull predicate&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class NullPredicate {
	
	Boolean not;

	public NullPredicate(Boolean not) {
		super();
		this.not = not;
	}

	public Boolean getNot() {
		return not;
	}
	
}
