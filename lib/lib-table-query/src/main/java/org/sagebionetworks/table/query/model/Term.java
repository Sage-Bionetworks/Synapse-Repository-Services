package org.sagebionetworks.table.query.model;

import java.util.LinkedList;
import java.util.List;

/**
 * Term ::= {@link Factor} ({@link FactorPrime})*
 * <p>
 * The 'term' element in the BNF is defined with left-recursion. The
 * left-recursion was eliminated by transforming the right-hand-side to an
 * optional loop of FactorPrime products.
 *
 */
public class Term extends SQLElement {

	private Factor factor;
	private List<FactorPrime> primeList;

	public Term(Factor factor) {
		this.factor = factor;
		this.primeList = new LinkedList<FactorPrime>();
	}
	

	public Factor getFactor() {
		return factor;
	}
	
	/**
	 * Add a new factor prime to this term.
	 * @param prime
	 */
	public void addFactorPrime(FactorPrime prime){
		this.primeList.add(prime);
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		factor.toSql(builder, parameters);
		for(FactorPrime prime: primeList){
			prime.toSql(builder, parameters);
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, factor);
		for(FactorPrime prime: primeList){
			checkElement(elements, type, prime);
		}
	}
	
	@Override
	public Iterable<Element> children() {
		LinkedList<Element> list = new LinkedList<Element>();
		list.add(factor);
		list.addAll(primeList);
		return list;
	}
}
