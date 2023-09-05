package org.sagebionetworks.table.query.model;

import java.util.Collections;


public class BooleanFunctionPredicate extends SQLElement implements HasPredicate{

	final BooleanFunction booleanFunction;
	final PredicateLeftHandSide leftHandSide;

	public BooleanFunctionPredicate(BooleanFunction booleanFunction, ColumnReference columnReference) {
		this.booleanFunction = booleanFunction;
		this.leftHandSide = new PredicateLeftHandSide(columnReference);
	}

	public BooleanFunction getBooleanFunction() {
		return booleanFunction;
	}
	
	public ColumnReference getColumnReference() {
		return (ColumnReference) leftHandSide.getChild();
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(booleanFunction.name());
		builder.append("(");
		leftHandSide.toSql(builder, parameters);
		builder.append(")");
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(leftHandSide);
	}

	@Override
	public PredicateLeftHandSide getLeftHandSide() {
		return leftHandSide;
	}

	@Override
	public Iterable<UnsignedLiteral> getRightHandSideValues() {
		//nothing on right hand side
		return Collections.emptyList();
	}
}
