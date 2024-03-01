package org.sagebionetworks.table.query.model;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This matches &ltboolean term&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class BooleanTerm extends SQLElement {

	private SortedSet<BooleanFactor> andBooleanFactors;

	public BooleanTerm() {
		this.andBooleanFactors = new TreeSet<>();
	}
	
	public BooleanTerm(List<BooleanFactor> list) {
		this.andBooleanFactors = new TreeSet<>(list);
	}

	public void addAndBooleanFactor(BooleanFactor booleanFactor){
		this.andBooleanFactors.add(booleanFactor);
	}

	public SortedSet<BooleanFactor> getAndBooleanFactors() {
		return andBooleanFactors;
	}
	
	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		boolean isFirst = true;
		for(BooleanFactor booleanFactor: andBooleanFactors){
			if (!isFirst) {
				builder.append(" AND ");
			}
			booleanFactor.toSql(builder, parameters);
			isFirst = false;
		}
	}
	
	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(andBooleanFactors);
	}
}
