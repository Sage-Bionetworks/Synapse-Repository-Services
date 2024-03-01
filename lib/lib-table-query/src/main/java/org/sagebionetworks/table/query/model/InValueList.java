package org.sagebionetworks.table.query.model;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This matches &ltin value list&gt in:
 * <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class InValueList extends SQLElement {

	private SortedSet<ValueExpression> valueExpressions;

	public InValueList() {
		this.valueExpressions = new TreeSet<>();
	}

	public InValueList(List<ValueExpression> list) {
		this.valueExpressions = new TreeSet<>(list);
	}

	public void addValueExpression(ValueExpression valueExpression) {
		this.valueExpressions.add(valueExpression);
	}

	public SortedSet<ValueExpression> getValueExpressions() {
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
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(valueExpressions);
	}
}
