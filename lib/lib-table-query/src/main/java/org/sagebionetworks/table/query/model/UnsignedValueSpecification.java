package org.sagebionetworks.table.query.model;

/**
 * This matches &ltunsigned value specification&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class UnsignedValueSpecification {

	UnsignedLiteral unsignedLiteral;

	public UnsignedValueSpecification(UnsignedLiteral unsignedLiteral) {
		super();
		this.unsignedLiteral = unsignedLiteral;
	}

	public UnsignedLiteral getUnsignedLiteral() {
		return unsignedLiteral;
	}
	
}
