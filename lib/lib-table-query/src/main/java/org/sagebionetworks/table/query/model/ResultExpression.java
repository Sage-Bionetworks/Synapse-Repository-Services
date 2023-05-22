package org.sagebionetworks.table.query.model;

/**
 * ResultExpression ::= {@link ValueExpression}
 *
 */
public class ResultExpression extends SimpleBranch {

	public ResultExpression(ValueExpression child) {
		super(child);
	}

}
