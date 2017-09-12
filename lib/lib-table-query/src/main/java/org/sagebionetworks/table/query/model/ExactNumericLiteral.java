package org.sagebionetworks.table.query.model;

import java.util.List;

public class ExactNumericLiteral extends SQLElement {

	private UnsignedInteger leftUnsignedInteger;
	private boolean period;
	private UnsignedInteger rightUnsignedInteger;
	

	public ExactNumericLiteral(UnsignedInteger leftUnsignedInteger,
			boolean period, UnsignedInteger rightUnsignedInteger) {
		super();
		this.leftUnsignedInteger = leftUnsignedInteger;
		this.period = period;
		this.rightUnsignedInteger = rightUnsignedInteger;
	}

	@Override
	public void toSql(StringBuilder builder) {
		if(leftUnsignedInteger != null){
			leftUnsignedInteger.toSql(builder);
		}
		if(period){
			builder.append(".");
		}
		if(rightUnsignedInteger != null){
			rightUnsignedInteger.toSql(builder);
		}

	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, leftUnsignedInteger);
		checkElement(elements, type, rightUnsignedInteger);
	}

}
