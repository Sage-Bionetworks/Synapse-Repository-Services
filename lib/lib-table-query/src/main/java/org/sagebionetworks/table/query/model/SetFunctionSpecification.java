package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.query.model.visitors.ColumnTypeVisitor;
import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor.SQLClause;
import org.sagebionetworks.table.query.model.visitors.Visitor;

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

	public void visit(Visitor visitor) {
		if (countAsterisk == null) {
			visit(this.valueExpression, visitor);
		}
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		if (countAsterisk != null) {
			visitor.append("COUNT(*)");
		} else {
			visitor.append(setFunctionType.name());
			visitor.append("(");
			if (setQuantifier != null) {
				visitor.append(setQuantifier.name());
				visitor.append(" ");
			}
			visitor.pushCurrentClause(SQLClause.FUNCTION_PARAMETER);
			visit(this.valueExpression, visitor);
			visitor.popCurrentClause(SQLClause.FUNCTION_PARAMETER);
			visitor.append(")");
		}
	}

	public void visit(ColumnTypeVisitor visitor) {
		if (countAsterisk != null) {
			visitor.setColumnType(ColumnType.INTEGER);
		} else {
			switch (setFunctionType) {
			case COUNT:
				visitor.setColumnType(ColumnType.INTEGER);
				break;
			case MAX:
			case MIN:
				// the type is the type of the underlying value
				visit(valueExpression, visitor);
				break;
			case SUM:
				// the type is the type of the underlying value, only valid for numbers
				visit(valueExpression, visitor);
				if (visitor.getColumnType() != null
						&& !(visitor.getColumnType() == ColumnType.DOUBLE || visitor.getColumnType() == ColumnType.INTEGER)) {
					throw new IllegalArgumentException("Cannot calculate " + setFunctionType.name() + " for type "
							+ visitor.getColumnType().name());
				}
				break;
			case AVG:
				// averages for integers actually come back as doubles
				visitor.setColumnType(ColumnType.DOUBLE);
				break;
			default:
				throw new IllegalArgumentException("unhandled set function type");
			}
		}
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
