package org.sagebionetworks.table.query.model;

/**
 * Note: In sql-92.bnf this object is defined as:
 * <p>
 * CaseSpecification ::= {@link SimpleCase} | {@link SearchedCase}
 * 
 * However, both SimleCase and SearchedCase started with CASE and end with END which is ambiguous.
 * To resolve this ambiguity we redefined this object as:
 * <p>
 *  CaseSpecification ::= <CASE> ( {@link SimpleCase} | {@link SearchedCase} ) <END>
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
