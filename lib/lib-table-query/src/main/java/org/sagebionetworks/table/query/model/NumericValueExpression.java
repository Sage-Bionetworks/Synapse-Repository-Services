package org.sagebionetworks.table.query.model;

import java.util.List;

public class NumericValueExpression extends SQLElement {

	private Term term;

	public NumericValueExpression(Term term) {
		this.term = term;
	}

	public Term getTerm() {
		return term;
	}

	@Override
	public void toSql(StringBuilder builder) {
		term.toSql(builder);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, term);
	}
}
