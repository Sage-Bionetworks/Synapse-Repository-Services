package org.sagebionetworks.table.query.model;

import java.util.LinkedList;
import java.util.List;

/**
 * This matches &ltsearch condition&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class SearchCondition {
	
	List<BooleanTerm> orBooleanTerms;

	public SearchCondition() {
		super();
		this.orBooleanTerms = new LinkedList<BooleanTerm>();
	}
	
	public void addOrBooleanTerm(BooleanTerm orBooleanTerms){
		this.orBooleanTerms.add(orBooleanTerms);
	}

	public List<BooleanTerm> getOrBooleanTerms() {
		return orBooleanTerms;
	}
	
}
