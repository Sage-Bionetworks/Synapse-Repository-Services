package org.sagebionetworks.table.query.model;

/**
 * UnsignedValueSpecification ::= {@link UnsignedLiteral} | <general value specification>
 *
 */
public class UnsignedValueSpecification extends SimpleBranch {
	
	UnsignedLiteral unsignedLiteral;

	
	public UnsignedValueSpecification(UnsignedLiteral unsignedLiteral) {
		super(unsignedLiteral);
	}

}
