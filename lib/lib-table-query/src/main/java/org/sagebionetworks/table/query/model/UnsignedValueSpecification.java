package org.sagebionetworks.table.query.model;

/**
 * This matches &ltunsigned value specification&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class UnsignedValueSpecification implements SQLElement {

	UnsignedLiteral unsignedLiteral;

	public UnsignedValueSpecification(UnsignedLiteral unsignedLiteral) {
		super();
		this.unsignedLiteral = unsignedLiteral;
	}

	public UnsignedLiteral getUnsignedLiteral() {
		return unsignedLiteral;
	}

	@Override
	public void toSQL(StringBuilder builder) {
		this.unsignedLiteral.toSQL(builder);
	}
	
}
