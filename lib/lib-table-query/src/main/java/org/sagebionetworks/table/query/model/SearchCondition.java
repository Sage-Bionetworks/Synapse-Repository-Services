package org.sagebionetworks.table.query.model;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This matches &ltsearch condition&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class SearchCondition extends SQLElement {
	
	private SortedSet<BooleanTerm> orBooleanTerms;

	public SearchCondition() {
		super();
		this.orBooleanTerms = new TreeSet<>();
	}
	
	public SearchCondition(List<BooleanTerm> terms) {
		this.orBooleanTerms = new TreeSet<>(terms);
	}

	public void addOrBooleanTerm(BooleanTerm orBooleanTerms){
		this.orBooleanTerms.add(orBooleanTerms);
	}

	public SortedSet<BooleanTerm> getOrBooleanTerms() {
		return orBooleanTerms;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		boolean isFirst = true;
		
		for (BooleanTerm booleanTerm : orBooleanTerms) {
			if (!isFirst) {
				builder.append(" OR ");
			}
			booleanTerm.toSql(builder, parameters);
			isFirst = false;
		}
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(orBooleanTerms);
	}
	
}
