package org.sagebionetworks.table.query.model;

import com.sun.tools.javac.parser.Tokens;

/**
 * 
 * NumericValueFunction ::= {@link MySqlFunction}
 *
 */
public class NumericValueFunction extends SimpleBranch {
	

	public NumericValueFunction(MySqlFunction mysqlFunction){
		super(mysqlFunction);
	}

	public NumericValueFunction(CurrentUserFunction currentUserFunction) {
		super(currentUserFunction);
	}

    /*
	public NumericValueFunction(SynapseFunction synapseFunction){
		super(synapseFunction);
	} */
}
