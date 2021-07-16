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
	 * @return the primeList
	 */
	public List<FactorPrime> getPrimeList() {
		return primeList;
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
	public Iterable<Element> getChildren() {
		LinkedList<Element> list = new LinkedList<Element>();
		list.add(factor);
		list.addAll(primeList);
		return list;
	}
}
