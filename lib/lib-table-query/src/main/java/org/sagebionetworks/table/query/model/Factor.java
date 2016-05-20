package org.sagebionetworks.table.query.model;

import java.util.List;

public class Factor extends SQLElement {

	private NumericPrimary numericPrimary;

	public Factor(NumericPrimary numericPrimary) {
		this.numericPrimary = numericPrimary;
	}

	public NumericPrimary getNumericPrimary() {
		return numericPrimary;
	}

	@Override
	public void toSql(StringBuilder builder) {
		numericPrimary.toSql(builder);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, numericPrimary);
	}
}
