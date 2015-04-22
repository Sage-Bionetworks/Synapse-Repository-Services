package org.sagebionetworks.table.query.model;

import org.sagebionetworks.table.query.model.visitors.Visitor;

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

	public StringValueExpression getStringValueExpression() {
		return stringValueExpression;
	}

	public NumericValueExpression getNumericValueExpression() {
		return numericValueExpression;
	}

	public void visit(Visitor visitor) {
		if (this.stringValueExpression != null) {
			visit(this.stringValueExpression, visitor);
		} else {
			visit(this.numericValueExpression, visitor);
		}
	}
}
