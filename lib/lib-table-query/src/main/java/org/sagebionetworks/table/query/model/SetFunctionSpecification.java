package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * This matches &ltset function specification&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class SetFunctionSpecification extends SQLElement implements HasAggregate, HasFunctionType {
	
	Boolean countAsterisk;
	SetFunctionType setFunctionType;
	SetQuantifier setQuantifier;
	ValueExpression valueExpression;
	
	public SetFunctionSpecification(Boolean countAsterisk) {
		this.countAsterisk = countAsterisk;
	}
	
	public SetFunctionSpecification(SetFunctionType setFunctionType, SetQuantifier setQuantifier, ValueExpression valueExpression) {
		this.setFunctionType = setFunctionType;
		this.setQuantifier = setQuantifier;
		this.valueExpression = valueExpression;
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

	@Override
	public void toSql(StringBuilder builder) {
		if (countAsterisk != null) {
			builder.append("COUNT(*)");
		} else {
			builder.append(setFunctionType.name());
			builder.append("(");
			if (setQuantifier != null) {
				builder.append(setQuantifier.name());
				builder.append(" ");
			}
			valueExpression.toSql(builder);
			builder.append(")");
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, valueExpression);
	}

	@Override
	public boolean isElementAggregate() {
		return true;
	}

	@Override
	public FunctionType getFunctionType() {
		if(countAsterisk != null){
			return FunctionType.COUNT;
		}
		// Switch by type.
		switch (setFunctionType) {
		case COUNT:
			return FunctionType.COUNT;
		case MAX:
			return FunctionType.MAX;
		case MIN:
			return FunctionType.MIN;
		case SUM:
			return FunctionType.SUM;
		case AVG:
			return FunctionType.AVG;
		default:
			throw new IllegalArgumentException("unhandled set function type");
		}
	}
}
