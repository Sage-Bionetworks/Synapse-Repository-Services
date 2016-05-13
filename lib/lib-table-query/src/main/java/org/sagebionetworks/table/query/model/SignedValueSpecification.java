package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.table.query.model.visitors.Visitor;

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

	public void visit(Visitor visitor) {
		visit(this.signedLiteral, visitor);
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
