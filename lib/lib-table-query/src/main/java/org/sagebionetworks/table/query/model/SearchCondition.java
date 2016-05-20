package org.sagebionetworks.table.query.model;

import java.util.LinkedList;
import java.util.List;

/**
 * This matches &ltsearch condition&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class SearchCondition extends SQLElement {
	
	List<BooleanTerm> orBooleanTerms;

	public SearchCondition() {
		super();
		this.orBooleanTerms = new LinkedList<BooleanTerm>();
	}
	
	public SearchCondition(List<BooleanTerm> terms) {
		this.orBooleanTerms = terms;
	}

	public void addOrBooleanTerm(BooleanTerm orBooleanTerms){
		this.orBooleanTerms.add(orBooleanTerms);
	}

	public List<BooleanTerm> getOrBooleanTerms() {
		return orBooleanTerms;
	}

	@Override
	public void toSql(StringBuilder builder) {
		boolean isFirst = true;
		for (BooleanTerm booleanTerm : orBooleanTerms) {
			if (!isFirst) {
				builder.append(" OR ");
			}
			booleanTerm.toSql(builder);
			isFirst = false;
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		for (BooleanTerm booleanTerm : orBooleanTerms) {
			checkElement(elements, type, booleanTerm);
		}
	}
}
