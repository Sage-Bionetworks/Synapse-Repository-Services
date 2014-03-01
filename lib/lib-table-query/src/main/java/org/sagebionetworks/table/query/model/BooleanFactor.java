package org.sagebionetworks.table.query.model;

/**
 * This matches &ltboolean factor&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class BooleanFactor {

	Boolean not;
	BooleanTest booleanTest;
	
	public BooleanFactor(Boolean not, BooleanTest booleanTest) {
		super();
		this.not = not;
		this.booleanTest = booleanTest;
	}
	public Boolean getNot() {
		return not;
	}
	public BooleanTest getBooleanTest() {
		return booleanTest;
	}
	
}
