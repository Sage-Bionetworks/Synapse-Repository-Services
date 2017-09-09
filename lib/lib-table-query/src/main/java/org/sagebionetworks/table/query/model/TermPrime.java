package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * Term prime is used to support arithmetic operations of terms while
 * avoiding left-recursion.
 *
 */
public class TermPrime extends SQLElement {

	private ArithmeticOperator operator;
	private Term term;

	/**
	 * Both the operator and term are required for a term prime.
	 * 
	 * @param operator
	 * @param term
	 */
	public TermPrime(ArithmeticOperator operator, Term term) {
		super();
		this.operator = operator;
		this.term = term;
	}

	@Override
	public void toSql(StringBuilder builder) {
		builder.append(operator.toSQL());
		term.toSql(builder);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, term);
	}

}
