package org.sagebionetworks.table.query.model;

public class ApproximateNumericLiteral extends LeafElement {
	
	Double doubleValue;

	public ApproximateNumericLiteral(String approximateNumericLiteral) {
		super();
		this.doubleValue = new Double(approximateNumericLiteral);
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(doubleValue);
	}

}
