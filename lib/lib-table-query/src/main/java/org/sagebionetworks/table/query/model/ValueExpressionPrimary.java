package org.sagebionetworks.table.query.model;

import java.util.List;

/**
 * ValueExpressionPrimary ::= {@link UnsignedValueSpecification} | {@link ColumnReference} | {@link SetFunctionSpecification}
 */
public class ValueExpressionPrimary extends SQLElement implements HasReferencedColumn {

	UnsignedValueSpecification unsignedValueSpecifictation;
	ColumnReference columnReference;
	SetFunctionSpecification setFunctionSpecification;
	
	public ValueExpressionPrimary(UnsignedValueSpecification unsignedValueSpecifictation) {
		this.unsignedValueSpecifictation = unsignedValueSpecifictation;
	}
	
	public ValueExpressionPrimary(ColumnReference columnReference) {
		this.columnReference = columnReference;
	}

	public ValueExpressionPrimary(SetFunctionSpecification setFunctionSpecification) {
		this.setFunctionSpecification = setFunctionSpecification;
	}

	public ColumnReference getColumnReference() {
		return columnReference;
	}
	public SetFunctionSpecification getSetFunctionSpecification() {
		return setFunctionSpecification;
	}

	@Override
	public void toSql(StringBuilder builder, ToSqlParameters parameters) {
		// only one element at a time will be no null
		if (unsignedValueSpecifictation != null) {
			unsignedValueSpecifictation.toSql(builder, parameters);
		} else if (columnReference != null) {
			columnReference.toSql(builder, parameters);
		} else {
			setFunctionSpecification.toSql(builder, parameters);
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, unsignedValueSpecifictation);
		checkElement(elements, type, columnReference);
		checkElement(elements, type, setFunctionSpecification);
	}

	@Override
	public ColumnNameReference getReferencedColumn() {
		// Handle functions first
		if(setFunctionSpecification != null){
			if(setFunctionSpecification.getCountAsterisk() != null){
				// count(*) does not reference a column
				return null;
			}else{
				// first unquoted value starting at the value expression.
				return setFunctionSpecification.getValueExpression().getFirstElementOfType(ColumnNameReference.class);
			}
		}else{
			// This is not a function so get the first unquoted.
			return this.getFirstElementOfType(ColumnNameReference.class);
		}
	}

	@Override
	public boolean isReferenceInFunction() {
		if(setFunctionSpecification != null && setFunctionSpecification.getCountAsterisk() != null){
			throw new IllegalArgumentException("COUNT(*) does not have a column reference");
		}
		return setFunctionSpecification != null;
	}

	
	
}
