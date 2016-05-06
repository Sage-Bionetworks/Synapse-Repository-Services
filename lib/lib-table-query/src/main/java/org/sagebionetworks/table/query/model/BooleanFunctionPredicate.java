package org.sagebionetworks.table.query.model;

import java.util.List;

import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.ToTranslatedSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;


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

	public void visit(Visitor visitor) {
		visit(columnReference, visitor);
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		visitor.append(booleanFunction.name());
		visitor.append("(");
		visit(columnReference, visitor);
		visitor.append(")");
	}

	public void visit(ToTranslatedSqlVisitor visitor) {
		visitor.handleFunction(booleanFunction, columnReference);
	}

	@Override
	public void toSql(StringBuilder builder) {
		builder.append(booleanFunction.name());
		builder.append("(");
		columnReference.toSql(builder);
		builder.append(")");
	}

	@Override
	<T extends Element> void addElements(List<T> elements, Class<T> type) {
		checkElement(elements, type, columnReference);
	}
}
