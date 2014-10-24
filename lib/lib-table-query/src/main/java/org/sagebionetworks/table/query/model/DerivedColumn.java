package org.sagebionetworks.table.query.model;


/**
 * This matches &ltderived column&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class DerivedColumn extends SQLElement {

	ValueExpression valueExpression;
	AsClause asClause;
	
	public DerivedColumn(ValueExpression valueExpression, AsClause asClause) {
		super();
		this.valueExpression = valueExpression;
		this.asClause = asClause;
	}

	public boolean isAggregate() {
		return valueExpression.isAggregate();
	}

	public ValueExpression getValueExpression() {
		return valueExpression;
	}

	public AsClause getAsClause() {
		return asClause;
	}

	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		valueExpression.toSQL(builder, columnConvertor);
		if(asClause!= null){
			builder.append(" ");
			asClause.toSQL(builder, columnConvertor);
		}
	}
	
}
