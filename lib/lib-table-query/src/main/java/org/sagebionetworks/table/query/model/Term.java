package org.sagebionetworks.table.query.model;


public class Term extends SQLElement {

	private Factor factor;

	public Term(Factor factor) {
		this.factor = factor;
	}

	public Factor getFactor() {
		return factor;
	}

	public void visit(Visitor visitor) {
		visit(this.factor, visitor);
	}
}
