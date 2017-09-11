package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * Factor prime is used to support arithmetic operations of factors while
 * avoiding left-recursion.
 *
 */
public class FactorPrime extends SQLElement {

	private ArithmeticOperator operator;
	private Factor factor;
	
	
	/**
	 * Both the operator and factor are required for a factor prime.
	 * @param operator
	 * @param factor
	 */
	public FactorPrime(ArithmeticOperator operator, Factor factor) {
		super();
		this.operator = operator;
		this.factor = factor;
	}
	@Override
	public void toSql(StringBuilder builder) {
		builder.append(operator.toSQL());
		factor.toSql(builder);
	}
	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, factor);
	}

}
