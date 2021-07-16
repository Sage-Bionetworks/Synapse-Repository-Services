package org.sagebionetworks.table.query.model;

import java.util.LinkedList;
import java.util.List;

/**
 * NumericValueExpression ::= {@link Term} ({@link TermPrime})*
 * <p>
 * The 'numeric value expression' element in the BNF is defined with
 * left-recursion. The left-recursion was eliminated by transforming the
 * right-hand-side to an optional list of TermPrim products.
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
	 * @return the primeList
	 */
	public List<TermPrime> getPrimeList() {
		return primeList;
	}

	/**
	 * Add a new term prime to the list.
	 * @param prime
	 */
	public void addTermPrime(TermPrime prime){
		this.primeList.add(prime);
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		term.toSql(builder, parameters);
		for(TermPrime prime: primeList){
			prime.toSql(builder, parameters);
		}
	}
	
	@Override
	public Iterable<Element> getChildren() {
		LinkedList<Element> list = new LinkedList<Element>();
		list.add(term);
		list.addAll(primeList);
		return list;
	}
	
}
