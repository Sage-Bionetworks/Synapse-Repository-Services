package org.sagebionetworks.table.query.model;

import java.util.LinkedList;
import java.util.List;

/**
 * This matches &ltboolean term&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class BooleanTerm extends SQLElement {

	List<BooleanFactor> andBooleanFactors;

	public BooleanTerm() {
		this.andBooleanFactors = new LinkedList<BooleanFactor>();
	}
	
	public BooleanTerm(List<BooleanFactor> list) {
		this.andBooleanFactors = list;
	}

	public void addAndBooleanFactor(BooleanFactor booleanFactor){
		this.andBooleanFactors.add(booleanFactor);
	}

	public List<BooleanFactor> getAndBooleanFactors() {
		return andBooleanFactors;
	}
	
	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		boolean isFrist = true;
		for(BooleanFactor booleanFactor: andBooleanFactors){
			if(!isFrist){
				builder.append(" AND ");
			}
			booleanFactor.toSQL(builder, columnConvertor);
			isFrist = false;
		}
	}
}
