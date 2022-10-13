package org.sagebionetworks.table.query.model;

/**
 * NonJoinQueryExpression ::= {@link NonJoinQueryTerm} ( UNION [ ALL | DISTINCT
 * ] {@link NonJoinQueryTerm} )*
 * <p>
 * Note: This is modified from the BNF to support recursion in a manner
 * compatible with Java CC.
 */
public class NonJoinQueryExpression extends SQLElement {

	private SQLElement leftHandSide;
	private SetQuantifier setQuantifier;
	private NonJoinQueryTerm rightHandSide;

	public NonJoinQueryExpression(NonJoinQueryTerm leftHandSide) {
		super();
		this.leftHandSide = leftHandSide;
		this.setQuantifier = null;
		this.rightHandSide = null;
	}

	public NonJoinQueryExpression(NonJoinQueryExpression leftHandSide, SetQuantifier setQuantifier,
			NonJoinQueryTerm rightHandSide) {
		super();
		this.leftHandSide = leftHandSide;
		this.setQuantifier = setQuantifier;
		this.rightHandSide = rightHandSide;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		leftHandSide.toSql(builder, parameters);
		if (rightHandSide != null) {
			builder.append(" UNION");
			if (setQuantifier != null) {
				builder.append(" ").append(setQuantifier.name());
			}
			builder.append(" ");
			rightHandSide.toSql(builder, parameters);
		}
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(leftHandSide, rightHandSide);
	}

}
