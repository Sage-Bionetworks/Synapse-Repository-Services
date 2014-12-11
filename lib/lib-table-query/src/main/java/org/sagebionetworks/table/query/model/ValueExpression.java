package org.sagebionetworks.table.query.model;


/**
 * This matches &ltvalue expression&gt in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class ValueExpression extends SQLElement {

	private StringValueExpression stringValueExpression = null;
	private NumericValueExpression numericValueExpression = null;

	public ValueExpression(StringValueExpression stringValueExpression) {
		this.stringValueExpression = stringValueExpression;
	}

	public ValueExpression(NumericValueExpression numericValueExpression) {
		this.numericValueExpression = numericValueExpression;
	}

	public boolean isAggregate() {
		if (stringValueExpression != null) {
			return stringValueExpression.isAggregate();
		} else {
			return numericValueExpression.isAggregate();
		}
	}

	public StringValueExpression getStringValueExpression() {
		return stringValueExpression;
	}

	public NumericValueExpression getNumericValueExpression() {
		return numericValueExpression;
	}

	@Override
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		if (this.stringValueExpression != null) {
			this.stringValueExpression.toSQL(builder, columnConvertor);
		} else {
			this.numericValueExpression.toSQL(builder, columnConvertor);
		}
	}
}
