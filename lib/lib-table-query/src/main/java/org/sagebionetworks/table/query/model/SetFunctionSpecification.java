package org.sagebionetworks.table.query.model;

/**
 * This matches &ltset function specification&gt   in: <a href="https://github.com/ronsavage/SQL/blob/master/sql-92.bnf">SQL-92</a>
 */
public class SetFunctionSpecification extends SQLElement implements HasAggregate, HasFunctionReturnType {
	
	Boolean countAsterisk;
	SetFunctionType setFunctionType;
	SetQuantifier setQuantifier;
	ValueExpressionList valueExpressionList;
	OrderByClause orderByClause;
	Separator separator;
	
	public SetFunctionSpecification(Boolean countAsterisk) {
		this.countAsterisk = countAsterisk;
		this.setFunctionType = SetFunctionType.COUNT;
	}
	
	public SetFunctionSpecification(SetFunctionType setFunctionType, SetQuantifier setQuantifier,
			ValueExpressionList valueExpressionList, OrderByClause orderByClause, Separator separator) {
		this.setFunctionType = setFunctionType;
		this.setQuantifier = setQuantifier;
		this.valueExpressionList = valueExpressionList;
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

	public ValueExpressionList getValueExpressionList() {
		return valueExpressionList;
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
			valueExpressionList.toSql(builder, parameters);
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
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(valueExpressionList, orderByClause, separator);
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
