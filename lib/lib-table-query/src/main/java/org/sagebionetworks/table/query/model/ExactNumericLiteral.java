package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * ExactNumericLiteral can be a Java Long or Double
 *
 */
public class ExactNumericLiteral extends SQLElement {
	
	Long longValue;
	Double doubleValue;
	
	public ExactNumericLiteral(Long longValue) {
		super();
		this.longValue = longValue;
	}
	
	public ExactNumericLiteral(Double doubleValue) {
		super();
		this.doubleValue = doubleValue;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		if(longValue != null){
			builder.append(longValue);
		}else{
			builder.append(doubleValue);
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		// no sub-elements.
	}

}
