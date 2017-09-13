package org.sagebionetworks.table.query.model;

import java.util.List;

public class ApproximateNumericLiteral extends SQLElement {
	
	Double approximateNumericLiteral;

	public ApproximateNumericLiteral(String approximateNumericLiteral) {
		super();
		this.approximateNumericLiteral = new Double(approximateNumericLiteral);
	}

	@Override
	public void toSql(StringBuilder builder) {
		builder.append(approximateNumericLiteral);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		// no sub-elements
	}

}
