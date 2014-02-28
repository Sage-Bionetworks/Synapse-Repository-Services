package org.sagebionetworks.table.query.model;

/**
 * This matches &ltin predicate value&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class InPredicateValue {

	InValueList inValueList;

	public InValueList getInValueList() {
		return inValueList;
	}
	
}
