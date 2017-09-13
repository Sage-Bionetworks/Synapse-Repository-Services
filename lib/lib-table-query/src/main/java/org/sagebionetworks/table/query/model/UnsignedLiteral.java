package org.sagebionetworks.table.query.model;

import java.util.List;

public class UnsignedLiteral extends SQLElement {
	
	private UnsignedNumericLiteral unsignedNumericLiteral;
	private GeneralLiteral generalLiteral;
	
	public UnsignedLiteral(UnsignedNumericLiteral unsignedNumericLiteral) {
		super();
		this.unsignedNumericLiteral = unsignedNumericLiteral;
	}
	
	public UnsignedLiteral(GeneralLiteral generalLiteral) {
		super();
		this.generalLiteral = generalLiteral;
	}

	@Override
	public void toSql(StringBuilder builder) {
		if(unsignedNumericLiteral != null){
			unsignedNumericLiteral.toSql(builder);
		} else {
			generalLiteral.toSql(builder);
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, unsignedNumericLiteral);
		checkElement(elements, type, generalLiteral);
	}

}
