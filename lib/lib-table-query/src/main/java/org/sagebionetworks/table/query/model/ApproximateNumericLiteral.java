package org.sagebionetworks.table.query.model;

import java.util.List;

public class ApproximateNumericLiteral extends SQLElement {
	
	Double doubleValue;

	public ApproximateNumericLiteral(String approximateNumericLiteral) {
		super();
		this.doubleValue = new Double(approximateNumericLiteral);
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(doubleValue);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		// no sub-elements
	}

}
