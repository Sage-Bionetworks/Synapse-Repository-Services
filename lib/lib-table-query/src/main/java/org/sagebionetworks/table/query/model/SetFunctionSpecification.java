package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * This matches &ltset function specification&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class SetFunctionSpecification extends SQLElement implements HasAggregate, HasFunctionReturnType {
	
	Boolean countAsterisk;
	SetFunctionType setFunctionType;
	SetQuantifier setQuantifier;
	ValueExpression valueExpression;
	OrderByClause orderByClause;
	Separator separator;
	
	public SetFunctionSpecification(Boolean countAsterisk) {
		this.countAsterisk = countAsterisk;
		this.setFunctionType = SetFunctionType.COUNT;
	}
	
	public SetFunctionSpecification(SetFunctionType setFunctionType, SetQuantifier setQuantifier,
			ValueExpression valueExpression, OrderByClause orderByClause, Separator separator) {
		this.setFunctionType = setFunctionType;
		this.setQuantifier = setQuantifier;
		this.valueExpression = valueExpression;
		this.orderByClause = orderByClause;
		this.separator = separator;
	}

	public Boolean getCountAsterisk() {
		return countAsterisk;
	}

	public SetFunctionType getSetFunctionType() {
		return setFunctionType;
	}

	public SetQuantifier getSetQuantifier() {
		return setQuantifier;
	}

	public ValueExpression getValueExpression() {
		return valueExpression;
	}
	
	public OrderByClause getOrderByClause() {
		return orderByClause;
	}

	public Separator getSeparator() {
		return separator;
	}
	
	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		if (countAsterisk != null) {
			builder.append("COUNT(*)");
		} else {
			builder.append(setFunctionType.name());
			builder.append("(");
			if (setQuantifier != null) {
				builder.append(setQuantifier.name());
				builder.append(" ");
			}
			valueExpression.toSql(builder, parameters);
			if(orderByClause != null) {
				builder.append(" ");
				orderByClause.toSql(builder, parameters);
			}
			if(separator != null) {
				builder.append(" ");
				separator.toSql(builder, parameters);
			}
			builder.append(")");
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, valueExpression);
		checkElement(elements, type, orderByClause);
		checkElement(elements, type, separator);
	}

	@Override
	public boolean isElementAggregate() {
		return true;
	}

	@Override
	public FunctionReturnType getFunctionReturnType() {
		return this.setFunctionType.getFunctionReturnType();
	}

}
