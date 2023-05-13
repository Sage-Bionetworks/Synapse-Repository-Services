package org.sagebionetworks.table.query.model;

/**
 * 
 * CaseSpecification ::= {@link SimpleCase} | {@link SearchedCase}
 *
 */
public class CaseSpecification extends SimpleBranch {

	public CaseSpecification(SimpleCase child) {
		super(child);
	}
	
	public CaseSpecification(SearchedCase child) {
		super(child);
	}

}
