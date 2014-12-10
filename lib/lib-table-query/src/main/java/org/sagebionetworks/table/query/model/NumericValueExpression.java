package org.sagebionetworks.table.query.model;


public class NumericValueExpression extends SQLElement {

	private Term term;

	public NumericValueExpression(Term term) {
		this.term = term;
	}

	public Term getTerm() {
		return term;
	}

	public void visit(Visitor visitor) {
		visit(this.term, visitor);
	}
}
