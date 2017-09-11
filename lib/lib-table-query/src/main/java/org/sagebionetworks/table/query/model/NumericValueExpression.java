package org.sagebionetworks.table.query.model;

import java.util.LinkedList;
import java.util.List;

/**
 * 
 * The 'numeric value expression' element in the BNF is defined with
 * left-recursion. The left-recursion was eliminated by transforming the
 * right-hand-side to an optional list of TermFactor products.
 *
 */
public class NumericValueExpression extends SQLElement {

	private Term term;
	private List<TermPrime> primeList;

	public NumericValueExpression(Term term) {
		this.term = term;
		this.primeList = new LinkedList<TermPrime>();
	}

	public Term getTerm() {
		return term;
	}
	
	/**
	 * Add a new term prime to the list.
	 * @param prime
	 */
	public void addTermPrime(TermPrime prime){
		this.primeList.add(prime);
	}

	@Override
	public void toSql(StringBuilder builder) {
		term.toSql(builder);
		for(TermPrime prime: primeList){
			prime.toSql(builder);
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, term);
		for(TermPrime prime: primeList){
			checkElement(elements, type, prime);
		}
	}
}
