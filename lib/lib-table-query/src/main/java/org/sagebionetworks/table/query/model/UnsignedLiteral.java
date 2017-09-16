package org.sagebionetworks.table.query.model;


/**
 * 
 * UnsignedLiteral ::= {@link UnsignedNumericLiteral} | {@link GeneralLiteral}
 * 
 */
public class UnsignedLiteral extends SimpleBranch implements ColumnNameReference {
		
	public UnsignedLiteral(UnsignedNumericLiteral unsignedNumericLiteral) {
		super(unsignedNumericLiteral);
	}
	
	public UnsignedLiteral(GeneralLiteral generalLiteral) {
		super(generalLiteral);
	}

}
