package org.sagebionetworks.table.query.model;

/**
 * 
 * <cast specification> ::= CAST <left paren> {@link CastOperand} AS {@link CastTarget}  <right paren>
 *
 */
public class CastSpecification extends SQLElement {

	private final CastOperand castOperand;
	private final CastTarget castTarget;

	public CastSpecification(CastOperand castOperand, CastTarget castTarget) {
		super();
		this.castOperand = castOperand;
		this.castTarget = castTarget;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("CAST(");
		castOperand.toSql(builder, parameters);
		builder.append(" AS ");
		castTarget.toSql(builder, parameters);
		builder.append(")");
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(castOperand, castTarget);
	}

}
