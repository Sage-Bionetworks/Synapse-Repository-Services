package org.sagebionetworks.table.query.model;

import java.util.List;

public class Term extends SQLElement {

	private Factor factor;

	public Term(Factor factor) {
		this.factor = factor;
	}

	public Factor getFactor() {
		return factor;
	}

	@Override
	public void toSql(StringBuilder builder) {
		factor.toSql(builder);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, factor);
	}
}
