package org.sagebionetworks.table.query.model;

/**
 * FactorPrime ::= {@link ArithmeticOperator} {@link Factor}
 * <p>
 * 
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
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(operator.toSQL());
		factor.toSql(builder, parameters);
	}
	
	@Override
	public Iterable<Element> children() {
		return SQLElement.buildChildren(factor);
	}

}
