package org.sagebionetworks.repo.model.query.entity;

import org.sagebionetworks.repo.model.query.Comparator;

/**
 * Represents a SQL expression:
 * left-hand-side comparator right-hand-side
 *
 */
public class SqlExpression extends SqlElement {
	
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
		if(leftHandSide.nodeToEntity == null){
			this.rightHandSide = rightHandSide;
		}else{
			// transform the right-hand-side
			this.rightHandSide = leftHandSide.nodeToEntity.transformerValue(rightHandSide);
		}

		this.bindName = Constants.BIND_PREFIX_EXPRESSION+leftHandSide.getColumnIndex();
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

	public ColumnReference getLeftHandSide() {
		return leftHandSide;
	}

	public Comparator getCompare() {
		return compare;
	}

	public Object getRightHandSide() {
		return rightHandSide;
	}

	public String getBindName() {
		return bindName;
	}
	
	
}
