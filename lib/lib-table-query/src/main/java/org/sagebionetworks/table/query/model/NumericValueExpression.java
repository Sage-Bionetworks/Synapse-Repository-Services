package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.table.query.model.visitors.Visitor;

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

	@Override
	public void toSql(StringBuilder builder) {
		term.toSql(builder);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, term);
	}
}
