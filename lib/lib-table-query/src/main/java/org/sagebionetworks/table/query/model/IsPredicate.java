package org.sagebionetworks.table.query.model;

import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;


/**
 * This matches &ltis predicate&gt in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public abstract class IsPredicate extends SQLElement {
	
	ColumnReference columnReferenceLHS;
	Boolean not;

	public IsPredicate(ColumnReference columnReferenceLHS, Boolean not) {
		this.columnReferenceLHS = columnReferenceLHS;
		this.not = not;
	}

	public Boolean getNot() {
		return not;
	}

	public ColumnReference getColumnReferenceLHS() {
		return columnReferenceLHS;
	}

	public abstract String getCompareValue();

	public void visit(Visitor visitor) {
		visit(columnReferenceLHS, visitor);
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		visit(columnReferenceLHS, visitor);
		visitor.append(" IS ");
		if (not != null) {
			visitor.append("NOT ");
		}
		visitor.append(getCompareValue());
	}
}
