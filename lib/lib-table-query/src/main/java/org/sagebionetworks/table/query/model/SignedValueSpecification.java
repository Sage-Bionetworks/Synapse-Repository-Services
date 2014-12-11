package org.sagebionetworks.table.query.model;


/**
 * This matches &lt;signed value specification&gt; in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class SignedValueSpecification extends SQLElement {

	SignedLiteral signedLiteral;

	public SignedValueSpecification(SignedLiteral signedLiteral) {
		super();
		this.signedLiteral = signedLiteral;
	}

	public SignedLiteral getSignedLiteral() {
		return signedLiteral;
	}

	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		this.signedLiteral.toSQL(builder, columnConvertor);
	}
	
}
