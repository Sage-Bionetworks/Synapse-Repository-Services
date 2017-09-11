package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * This matches &lt;signed value specification&gt; in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
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
	public void toSql(StringBuilder builder) {
		signedLiteral.toSql(builder);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, signedLiteral);
	}
}
