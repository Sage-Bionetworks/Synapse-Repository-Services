package org.sagebionetworks.table.query.model;

/**
 * CaseOperand ::= {@link ValueExpression}
 *
 */
public class CaseOperand  extends SimpleBranch {

	public CaseOperand(ValueExpression child) {
		super(child);
	}

}
