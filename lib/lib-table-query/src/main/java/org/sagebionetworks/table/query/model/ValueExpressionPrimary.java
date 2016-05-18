package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.table.query.model.visitors.Visitor;

/**
 * This matches &ltvalue expression primary&gt   in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class ValueExpressionPrimary extends SQLElement implements HasReferencedColumn {

	SignedValueSpecification signedValueSpecification;
	ColumnReference columnReference;
	SetFunctionSpecification setFunctionSpecification;
	
	public ValueExpressionPrimary(SignedValueSpecification signedValueSpecification) {
		this.signedValueSpecification = signedValueSpecification;
	}
	
	public ValueExpressionPrimary(ColumnReference columnReference) {
		this.columnReference = columnReference;
	}

	public ValueExpressionPrimary(SetFunctionSpecification setFunctionSpecification) {
		this.setFunctionSpecification = setFunctionSpecification;
	}

	public SignedValueSpecification getSignedValueSpecification() {
		return signedValueSpecification;
	}
	public ColumnReference getColumnReference() {
		return columnReference;
	}
	public SetFunctionSpecification getSetFunctionSpecification() {
		return setFunctionSpecification;
	}

	public void visit(Visitor visitor) {
		// only one element at a time will be no null
		if (signedValueSpecification != null) {
			visit(signedValueSpecification, visitor);
		} else if (columnReference != null) {
			visit(columnReference, visitor);
		} else {
			visit(setFunctionSpecification, visitor);
		}
	}

	@Override
	public void toSql(StringBuilder builder) {
		// only one element at a time will be no null
		if (signedValueSpecification != null) {
			signedValueSpecification.toSql(builder);
		} else if (columnReference != null) {
			columnReference.toSql(builder);
		} else {
			setFunctionSpecification.toSql(builder);
		}
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, signedValueSpecification);
		checkElement(elements, type, columnReference);
		checkElement(elements, type, setFunctionSpecification);
	}

	@Override
	public HasQuoteValue getReferencedColumn() {
		// Handle functions first
		if(setFunctionSpecification != null){
			if(setFunctionSpecification.getCountAsterisk() != null){
				// count(*) does not reference a column
				return null;
			}else{
				// first unquoted value starting at the value expression.
				return setFunctionSpecification.getValueExpression().getFirstElementOfType(HasQuoteValue.class);
			}
		}else{
			// This is not a function so get the first unquoted.
			return this.getFirstElementOfType(HasQuoteValue.class);
		}
	}

	
	
}
