package org.sagebionetworks.table.query.model;

import org.sagebionetworks.table.query.model.visitors.Visitor;

public class Factor extends SQLElement {

	private NumericPrimary numericPrimary;

	public Factor(NumericPrimary numericPrimary) {
		this.numericPrimary = numericPrimary;
	}

	public NumericPrimary getNumericPrimary() {
		return numericPrimary;
	}

	public void visit(Visitor visitor) {
		visit(this.numericPrimary, visitor);
	}
}
