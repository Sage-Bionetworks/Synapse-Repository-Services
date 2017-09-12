package org.sagebionetworks.table.query.model;

import java.util.List;

public class SignedInteger extends SQLElement {

	Sign sign;
	UnsignedInteger unsignedInteger;
	
	public SignedInteger(Sign sign, UnsignedInteger unsignedInteger) {
		super();
		this.sign = sign;
		this.unsignedInteger = unsignedInteger;
	}

	@Override
	public void toSql(StringBuilder builder) {
		if(sign != null){
			builder.append(sign.toSQL());
		}
		unsignedInteger.toSql(builder);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, unsignedInteger);
	}

}
