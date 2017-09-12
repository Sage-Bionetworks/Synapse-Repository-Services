package org.sagebionetworks.table.query.model;

import java.util.List;

public class UnsignedInteger extends SQLElement {
	
	Integer integer;
	
	public UnsignedInteger(String stringValues){
		integer = new Integer(stringValues);
	}

	@Override
	public void toSql(StringBuilder builder) {
		builder.append(integer);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		// no sub-elements
	}

}
