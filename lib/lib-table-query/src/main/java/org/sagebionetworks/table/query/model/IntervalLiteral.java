package org.sagebionetworks.table.query.model;

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

	
	/**
	 * @return the unsignedInteger
	 */
	public UnsignedInteger getUnsignedInteger() {
		return unsignedInteger;
	}

	/**
	 * @return the datetimeField
	 */
	public DatetimeField getDatetimeField() {
		return datetimeField;
	}



	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append("INTERVAL ");
		unsignedInteger.toSql(builder, parameters);
		builder.append(" ");
		builder.append(datetimeField.toString());
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(unsignedInteger);
	}

}
