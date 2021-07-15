package org.sagebionetworks.table.query.model;

/**
 * 
 * IntervalLiteral ::= INTERVAL {@link UnsignedInteger} {@link DatetimeField}
 *
 */
public class IntervalLiteral extends LeafElement {

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

}
