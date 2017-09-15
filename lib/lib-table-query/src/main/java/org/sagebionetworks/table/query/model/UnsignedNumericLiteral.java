package org.sagebionetworks.table.query.model;


/**
 * UnsignedNumericLiteral ::= {@link ExactNumericLiteral} | {@link ApproximateNumericLiteral}
 *
 */
public class UnsignedNumericLiteral extends SimpleBranch {

	public UnsignedNumericLiteral(ExactNumericLiteral exactNumericLiteral) {
		super(exactNumericLiteral);
	}
	
	public UnsignedNumericLiteral(
			ApproximateNumericLiteral approximateNumericLiteral) {
		super(approximateNumericLiteral);
	}

}
