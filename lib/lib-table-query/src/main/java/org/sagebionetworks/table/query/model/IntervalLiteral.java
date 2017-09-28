package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * 
 * IntervalLiteral ::= INTERVAL {@link UnsignedInteger} {@link DatetimeField}
 *
 */
public class IntervalLiteral extends SQLElement {

	UnsignedInteger unsignedInteger;
	DatetimeField datetimeField;
	
	public IntervalLiteral(UnsignedInteger unsignedInteger,
			DatetimeField datetimeField) {
		super();
		this.unsignedInteger = unsignedInteger;
		this.datetimeField = datetimeField;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("INTERVAL ");
		unsignedInteger.toSql(builder, parameters);
		builder.append(" ");
		builder.append(datetimeField.toString());
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		// this is a leaf
	}

}
