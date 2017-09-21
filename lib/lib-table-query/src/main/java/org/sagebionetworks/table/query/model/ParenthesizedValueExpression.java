package org.sagebionetworks.table.query.model;

/**
 * ParenthesizedValueExpression ::= '(' {@link ValueExpression} ')'
 *
 */
public class ParenthesizedValueExpression extends SimpleBranch {

	public ParenthesizedValueExpression(ValueExpression valueExpression) {
		super(valueExpression);
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("(");
		this.child.toSql(builder, parameters);
		builder.append(")");
	}
}
