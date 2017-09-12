package org.sagebionetworks.table.query.model;

import java.util.List;

public class ApproximateNumericLiteral extends SQLElement {
	
	ExactNumericLiteral mantissa;
	SignedInteger exponent;

	public ApproximateNumericLiteral(ExactNumericLiteral mantissa,
			SignedInteger exponent) {
		super();
		this.mantissa = mantissa;
		this.exponent = exponent;
	}

	@Override
	public void toSql(StringBuilder builder) {
		mantissa.toSql(builder);
		builder.append("E");
		exponent.toSql(builder);

	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, mantissa);
		checkElement(elements, type, exponent);
	}

}
