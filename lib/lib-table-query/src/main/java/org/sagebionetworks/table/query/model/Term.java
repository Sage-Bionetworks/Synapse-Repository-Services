package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.table.query.model.visitors.Visitor;

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

	@Override
	public void toSql(StringBuilder builder) {
		factor.toSql(builder);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, factor);
	}
}
