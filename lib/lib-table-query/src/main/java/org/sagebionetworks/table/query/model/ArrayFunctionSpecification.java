package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * Custom functions that operate on multi-value columns.
 * Does not match anything in the SQL-92 specification.
 *
 * <array function specification> ::= <array function type> <left paren> <column reference> <right paren>
 *
 * Example:
 * UNNEST(foo)
 *
 * See  https://sagebionetworks.jira.com/wiki/spaces/PLFM/pages/817168468/Multiple+Value+Annotations
 * Related: {@link ArrayHasPredicate}
 */
public class ArrayFunctionSpecification extends SQLElement implements HasFunctionReturnType{
	ArrayFunctionType listFunctionType;

	//For the time being, only support ColumnReferences because currently the only
	//type that returns a list is a column that is has a "*_LIST" type (e.g. "STRING_LIST")
	ColumnReference columnReference;

	public ArrayFunctionSpecification(ArrayFunctionType listFunctionType, ColumnReference columnReference) {
		this.listFunctionType = listFunctionType;
		this.columnReference = columnReference;
	}

	public ArrayFunctionType getListFunctionType() {
		return listFunctionType;
	}

	public ColumnReference getColumnReference() {
		return columnReference;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		builder.append(listFunctionType.name())
				.append("(");
		columnReference.toSql(builder, parameters);
		builder.append(")");
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, columnReference);
	}

	@Override
	public FunctionReturnType getFunctionReturnType() {
		return this.listFunctionType.getFunctionReturnType();
	}
}
