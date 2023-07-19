package org.sagebionetworks.table.query.model;

/**
 * PredicateLeftHandSide ::= {@link ColumnReference} | {@link MySqlFunction}
 */
public class PredicateLeftHandSide extends SimpleBranch {

	public PredicateLeftHandSide(ColumnReference columnReference) {
		super(columnReference);
	}
	
	public PredicateLeftHandSide(MySqlFunction mySqlFunction) {
		super(mySqlFunction);
	}

}
