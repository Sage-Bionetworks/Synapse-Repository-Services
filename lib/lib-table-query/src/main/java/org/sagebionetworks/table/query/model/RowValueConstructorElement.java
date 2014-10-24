package org.sagebionetworks.table.query.model;


/**
 * This matches &ltrow value constructor element&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class RowValueConstructorElement extends SQLElement {
	
	ValueExpression valueExpression = null;
	Boolean nullSpecification = null;
	TruthValue truthSpecification = null;
	Boolean defaultSpecification = null;
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

	public RowValueConstructorElement(TruthValue truthSpecification) {
		this.truthSpecification = truthSpecification;
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

	public TruthValue getTruthSpecification() {
		return truthSpecification;
	}
	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		if(valueExpression != null){
			valueExpression.toSQL(builder, columnConvertor);
		}else if(nullSpecification != null){
			builder.append("NULL");
		} else if (truthSpecification != null) {
			builder.append(truthSpecification.name());
		}else{
			builder.append("DEFAULT");
		}
	}
}
