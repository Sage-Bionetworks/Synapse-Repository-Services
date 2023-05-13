package org.sagebionetworks.table.query.model;

/**
 * CaseExpression ::= {@link CaseSpecification} | {@link CaseSpecification}
 *
 */
public class CaseExpression extends SimpleBranch {

	public CaseExpression(CaseSpecification child) {
		super(child);
	}

	public CaseExpression(CaseAbbreviation child) {
		super(child);
	}
}
