package org.sagebionetworks.table.query.model;

import org.sagebionetworks.table.query.model.visitors.ToSimpleSqlVisitor;
import org.sagebionetworks.table.query.model.visitors.Visitor;


/**
 * This matches &ltin predicate&gt  in: <a href="http://savage.net.au/SQL/sql-92.bnf">SQL-92</a>
 */
public class InPredicate extends SQLElement {

	ColumnReference columnReferenceLHS;
	Boolean not;
	InPredicateValue inPredicateValue;
	
	public InPredicate(ColumnReference columnReferenceLHS, Boolean not,
			InPredicateValue inPredicateValue) {
		super();
		this.columnReferenceLHS = columnReferenceLHS;
		this.not = not;
		this.inPredicateValue = inPredicateValue;
	}

	public Boolean getNot() {
		return not;
	}
	public InPredicateValue getInPredicateValue() {
		return inPredicateValue;
	}
	
	public ColumnReference getColumnReferenceLHS() {
		return columnReferenceLHS;
	}

	public void visit(Visitor visitor) {
		visit(columnReferenceLHS, visitor);
		visit(inPredicateValue, visitor);
	}

	public void visit(ToSimpleSqlVisitor visitor) {
		visit(columnReferenceLHS, visitor);
		visitor.setLHSColumn(columnReferenceLHS);
		visitor.append(" ");
		if (this.not != null) {
			visitor.append("NOT ");
		}
		visitor.append("IN ( ");
		visit(inPredicateValue, visitor);
		visitor.append(" )");
		visitor.setLHSColumn(null);
	}
}
