package org.sagebionetworks.table.query.model;

import java.util.LinkedList;
import java.util.List;

/**
 * This matches &ltin value list&gt in:
 * <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class InValueList extends SQLElement {

	List<ValueExpression> valueExpressions;

	public InValueList() {
		this.valueExpressions = new LinkedList<ValueExpression>();
	}

	public InValueList(List<ValueExpression> list) {
		this.valueExpressions = list;
	}

	public void addValueExpression(ValueExpression valueExpression) {
		this.valueExpressions.add(valueExpression);
	}

	public List<ValueExpression> getValueExpressions() {
		return valueExpressions;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		boolean first = true;
		for (ValueExpression valueExpression : valueExpressions) {
			if (!first) {
				builder.append(", ");
			}
			valueExpression.toSql(builder, parameters);
			first = false;
		}
	}

	@Override
	public Iterable<Element> children() {
		return SQLElement.buildChildren(valueExpressions);
	}
}
