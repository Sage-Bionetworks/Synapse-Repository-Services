package org.sagebionetworks.table.query.model;

/**
 * 
 * NumericValueFunction ::= {@link MySqlFunction}
 *
 */
public class NumericValueFunction extends SimpleBranch {
	

	public NumericValueFunction(MySqlFunction mysqlFuction){
		super(mysqlFuction);
	}
}
