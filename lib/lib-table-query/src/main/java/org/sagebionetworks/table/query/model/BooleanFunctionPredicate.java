package org.sagebionetworks.table.query.model;

import java.util.Collections;


public class BooleanFunctionPredicate extends SQLElement implements HasPredicate{

	final BooleanFunction booleanFunction;
	final ColumnReference columnReference;

	public BooleanFunctionPredicate(BooleanFunction booleanFunction, ColumnReference columnReference) {
		this.booleanFunction = booleanFunction;
		this.columnReference = columnReference;
	}

	public BooleanFunction getBooleanFunction() {
		return booleanFunction;
	}

	public ColumnReference getColumnReference() {
		return columnReference;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(booleanFunction.name());
		builder.append("(");
		columnReference.toSql(builder, parameters);
		builder.append(")");
	}

	@Override
	public Iterable<Element> getChildren() {
		return SQLElement.buildChildren(columnReference);
	}

	@Override
	public ColumnReference getLeftHandSide() {
		return columnReference;
	}

	@Override
	public Iterable<UnsignedLiteral> getRightHandSideValues() {
		//nothing on right hand side
		return Collections.emptyList();
	}
}
