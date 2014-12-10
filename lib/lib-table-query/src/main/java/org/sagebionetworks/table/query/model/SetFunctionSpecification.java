package org.sagebionetworks.table.query.model;

import org.sagebionetworks.table.query.model.ToSimpleSqlVisitor.SQLClause;

/**
 * This matches &ltset function specification&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class SetFunctionSpecification extends SQLElement {
	
	Boolean countAsterisk;
	SetFunctionType setFunctionType;
	SetQuantifier setQuantifier;
	ValueExpression valueExpression;
	
	public SetFunctionSpecification(Boolean countAsterisk) {
		super();
		this.countAsterisk = countAsterisk;
	}
	
	public SetFunctionSpecification(SetFunctionType setFunctionType,
			SetQuantifier setQuantifier, ValueExpression valueExpression) {
		super();
		this.setFunctionType = setFunctionType;
		this.setQuantifier = setQuantifier;
		this.valueExpression = valueExpression;
	}

	public boolean isAggregate() {
		return true;
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
}
