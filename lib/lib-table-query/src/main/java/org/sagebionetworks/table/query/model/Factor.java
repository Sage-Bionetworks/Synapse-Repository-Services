package org.sagebionetworks.table.query.model;


public class Factor extends SQLElement {

	private NumericPrimary numericPrimary;

	public Factor(NumericPrimary numericPrimary) {
		this.numericPrimary = numericPrimary;
	}

	public boolean isAggregate() {
		return numericPrimary.isAggregate();
	}

	public NumericPrimary getNumericPrimary() {
		return numericPrimary;
	}

	public void visit(Visitor visitor) {
		visit(this.numericPrimary, visitor);
	}
}
