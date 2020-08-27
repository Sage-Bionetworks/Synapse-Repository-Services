package org.sagebionetworks.table.query.model;


/**
 * 
 * NumericValueFunction ::= {@link MySqlFunction} | {@link CurrentUserFunction }
 *
 */
public class NumericValueFunction extends SimpleBranch {
	

	public NumericValueFunction(MySqlFunction mysqlFunction){
		super(mysqlFunction);
	}

	public NumericValueFunction(CurrentUserFunction currentUserFunction) {
		super(currentUserFunction);
	}

}
