package org.sagebionetworks.table.query.model;

import java.util.List;

public class UnsignedLiteral extends SQLElement {
	
	private UnsignedNumericLiteral unsignedNumericLiteral;
	

	public UnsignedLiteral(UnsignedNumericLiteral unsignedNumericLiteral) {
		super();
		this.unsignedNumericLiteral = unsignedNumericLiteral;
	}

	@Override
	public void toSql(StringBuilder builder) {
		unsignedNumericLiteral.toSql(builder);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, unsignedNumericLiteral);
	}

}
