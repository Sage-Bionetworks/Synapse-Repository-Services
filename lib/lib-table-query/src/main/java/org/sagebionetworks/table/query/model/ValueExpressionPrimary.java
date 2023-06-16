package org.sagebionetworks.table.query.model;


/**
 * ValueExpressionPrimary ::= {@link UnsignedValueSpecification} |
 * {@link ColumnReference} | {@link SetFunctionSpecification} |
 * {@link ParenthesizedValueExpression}
 * | {@link CaseExpression} | {@link CastSpecification}
 */
public class ValueExpressionPrimary extends SimpleBranch implements HasReferencedColumn {
	
	public ValueExpressionPrimary(UnsignedValueSpecification unsignedValueSpecifictation) {
		super(unsignedValueSpecifictation);
	}
	
	public ValueExpressionPrimary(ColumnReference columnReference) {
		super(columnReference);
	}

	public ValueExpressionPrimary(SetFunctionSpecification setFunctionSpecification) {
		super(setFunctionSpecification);
	}

	public ValueExpressionPrimary(ArrayFunctionSpecification arrayFunctionSpecification) {
		super(arrayFunctionSpecification);
	}

	public ValueExpressionPrimary(
			ParenthesizedValueExpression parenthesizedValueExpression) {
		super(parenthesizedValueExpression);
	}
	
	public ValueExpressionPrimary(CaseExpression caseExpression) {
		super(caseExpression);
	}
	
	public ValueExpressionPrimary(CastSpecification castSpecification) {
		super(castSpecification);
	}

	@Override
	public ColumnNameReference getReferencedColumn() {
		// Handle functions first
		SetFunctionSpecification setFunctionSpecification = this.getFirstElementOfType(SetFunctionSpecification.class);
		if(setFunctionSpecification != null){
			if(setFunctionSpecification.getCountAsterisk() != null){
				// count(*) does not reference a column
				return null;
			}else{
				// first unquoted value starting at the value expression.
				return setFunctionSpecification.getFirstElementOfType(ColumnNameReference.class);
			}
		}else{
			// This is not a function so get the first unquoted.
			return this.getFirstElementOfType(ColumnNameReference.class);
		}
	}

	@Override
	public boolean isReferenceInFunction() {
		SetFunctionSpecification setFunctionSpecification = this.getFirstElementOfType(SetFunctionSpecification.class);
		if(setFunctionSpecification != null && setFunctionSpecification.getCountAsterisk() != null){
			throw new IllegalArgumentException("COUNT(*) does not have a column reference");
		}
		return setFunctionSpecification != null;
	}

	
	
}
