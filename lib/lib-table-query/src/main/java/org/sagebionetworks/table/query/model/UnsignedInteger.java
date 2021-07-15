package org.sagebionetworks.table.query.model;

public class UnsignedInteger extends LeafElement {

	Long integer;

	public UnsignedInteger(String stringValues) {
		integer = new Long(stringValues);
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(integer);
	}

}
