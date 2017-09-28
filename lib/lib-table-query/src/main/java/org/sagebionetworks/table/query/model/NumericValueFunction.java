package org.sagebionetworks.table.query.model;

/**
 * 
 * NumericValueFunction ::= {@link MySqlFunction}
 *
 */
public class NumericValueFunction extends SimpleBranch implements HasAggregate {
	

	public NumericValueFunction(MySqlFunction mysqlFuction){
		super(mysqlFuction);
	}
	
	@Override
	public boolean isElementAggregate() {
		return true;
	}
}
