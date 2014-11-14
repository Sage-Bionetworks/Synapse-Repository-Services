package org.sagebionetworks.table.query.model;


public class BooleanFunctionPredicate extends SQLElement {

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
	public void toSQL(StringBuilder builder, ColumnConvertor columnConvertor) {
		if (columnConvertor != null) {
			columnConvertor.handleFunction(booleanFunction, columnReference, builder);
		} else {
			builder.append(booleanFunction.name());
			builder.append('(');
			columnReference.toSQL(builder, columnConvertor);
			builder.append(')');
		}
	}
}
