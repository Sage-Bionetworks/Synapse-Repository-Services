package org.sagebionetworks.table.query.model;

import java.util.List;

public class SignedNumericLiteral extends SQLElement {
	
	private Sign sign;
	private UnsignedNumericLiteral unsignedNumericLiteral;


	public SignedNumericLiteral(Sign sign,
			UnsignedNumericLiteral unsignedNumericLiteral) {
		super();
		this.sign = sign;
		this.unsignedNumericLiteral = unsignedNumericLiteral;
	}

	@Override
	public void toSql(StringBuilder builder) {
		if(sign != null){
			builder.append(sign.toSQL());
		}
		unsignedNumericLiteral.toSql(builder);
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, unsignedNumericLiteral);
	}

}
