package org.sagebionetworks.table.query.model;

import java.util.List;

public class Literal extends SQLElement {

	SignedNumericLiteral signedNumericLiteral;
	GeneralLiteral generalLiteral;
	
	public Literal(SignedNumericLiteral signedNumericLiteral) {
		super();
		this.signedNumericLiteral = signedNumericLiteral;
	}
		
	public Literal(GeneralLiteral generalLiteral) {
		super();
		this.generalLiteral = generalLiteral;
	}

	@Override
	public void toSql(StringBuilder builder) {
		if(signedNumericLiteral != null){
			signedNumericLiteral.toSql(builder);
		}else{
			generalLiteral.toSql(builder);
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, signedNumericLiteral);
		checkElement(elements, type, generalLiteral);
	}

}
