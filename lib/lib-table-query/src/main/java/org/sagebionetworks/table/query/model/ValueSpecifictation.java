package org.sagebionetworks.table.query.model;

import java.util.List;

public class ValueSpecifictation extends SQLElement {

	Literal literal;
	
	public ValueSpecifictation(Literal literal) {
		super();
		this.literal = literal;
	}

	@Override
	public void toSql(StringBuilder builder) {
		literal.toSql(builder);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, literal);
	}

}
