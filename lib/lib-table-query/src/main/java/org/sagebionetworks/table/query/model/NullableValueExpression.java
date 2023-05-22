package org.sagebionetworks.table.query.model;

/**
 * NullableValueExpression ::= NULL | {@link ValueExpression}
 */
public class NullableValueExpression extends SQLElement{
	
	private final ValueExpression valueExpression;
	
	public NullableValueExpression() {
		this.valueExpression = null;
	}

	public NullableValueExpression(ValueExpression valueExpression) {
		super();
		this.valueExpression = valueExpression;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		if(valueExpression != null) {
			valueExpression.toSql(builder, parameters);
		}else {
			builder.append("NULL");
		}
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(valueExpression);
	}

}
