package org.sagebionetworks.table.query.model;

import java.util.List;

public class UnsignedValueSpecification extends SQLElement {
	
	private UnsignedLiteral unsignedLiteral;

	public UnsignedValueSpecification(UnsignedLiteral unsignedLiteral) {
		super();
		this.unsignedLiteral = unsignedLiteral;
	}

	@Override
	public void toSql(StringBuilder builder) {
		unsignedLiteral.toSql(builder);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, unsignedLiteral);
	}

}
