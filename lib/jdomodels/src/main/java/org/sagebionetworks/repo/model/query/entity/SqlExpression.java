package org.sagebionetworks.repo.model.query.entity;

import org.sagebionetworks.repo.model.query.Comparator;

/**
 * Represents a SQL expression:
 * left-hand-side comparator right-hand-side
 *
 */
public class SqlExpression extends SqlElement {

	public static final String BIND_PREFIX = "bExpressionValue";
	
	ColumnReference leftHandSide;
	Comparator compare;
	Object rightHandSide;
	String bindName;
	
	/**
	 * Create a new SQL expression.
	 * 
	 * @param leftHandSide
	 * @param compare
	 * @param rightHandSide
	 * @param index
	 */
	public SqlExpression(ColumnReference leftHandSide, Comparator compare,
			Object rightHandSide) {
		super();
		this.leftHandSide = leftHandSide;
		this.compare = compare;
		this.rightHandSide = rightHandSide;
		this.bindName = BIND_PREFIX+leftHandSide.getColumnIndex();
	}

	@Override
	public void toSql(StringBuilder builder) {
		leftHandSide.toSql(builder);
		builder.append(" ");
		builder.append(compare.getSql());
		builder.append(" ");
		if(Comparator.IN.equals(compare)){
			builder.append("(");
		}
		builder.append(":");
		builder.append(bindName);
		if(Comparator.IN.equals(compare)){
			builder.append(")");
		}
	}

	@Override
	public void bindParameters(Parameters parameters) {
		parameters.put(bindName, rightHandSide);
	}
}
