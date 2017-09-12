package org.sagebionetworks.table.query.model;

import java.util.List;

public class UnsignedNumericLiteral extends SQLElement {

	private ExactNumericLiteral exactNumericLiteral;
	private ApproximateNumericLiteral approximateNumericLiteral;
	
	public UnsignedNumericLiteral(ExactNumericLiteral exactNumericLiteral) {
		super();
		this.exactNumericLiteral = exactNumericLiteral;
	}

	public UnsignedNumericLiteral(ApproximateNumericLiteral approximateNumericLiteral) {
		super();
		this.approximateNumericLiteral = approximateNumericLiteral;
	}

	@Override
	public void toSql(StringBuilder builder) {
		if(exactNumericLiteral != null){
			exactNumericLiteral.toSql(builder);
		}else{
			approximateNumericLiteral.toSql(builder);
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, exactNumericLiteral);
		checkElement(elements, type, approximateNumericLiteral);
	}

}
