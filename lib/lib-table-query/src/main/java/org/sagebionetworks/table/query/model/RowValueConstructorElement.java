package org.sagebionetworks.table.query.model;

/**
 * This matches &ltrow value constructor element&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class RowValueConstructorElement implements SQLElement {
	
	ValueExpression valueExpression;
	Boolean nullSpecification;
	Boolean defaultSpecification;
	public RowValueConstructorElement(ValueExpression valueExpression) {
		super();
		this.valueExpression = valueExpression;
	}
	public RowValueConstructorElement(Boolean nullSpecification,
			Boolean defaultSpecification) {
		super();
		this.nullSpecification = nullSpecification;
		this.defaultSpecification = defaultSpecification;
	}
	public ValueExpression getValueExpression() {
		return valueExpression;
	}
	public Boolean getNullSpecification() {
		return nullSpecification;
	}
	public Boolean getDefaultSpecification() {
		return defaultSpecification;
	}
	@Override
	public void toSQL(StringBuilder builder) {
		if(valueExpression != null){
			valueExpression.toSQL(builder);
		}else if(nullSpecification != null){
			builder.append("NULL");
		}else{
			builder.append("DEFAULT");
		}
	}
}
